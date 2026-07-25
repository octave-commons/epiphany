(ns epiphany.infra.http
  "HTTP API adapter using reitit + ring.

  Exposes the same command/query services the CLI uses via REST endpoints.
  Returns RFC 9457 problem+json for errors. JSON default, EDN accepted locally.
  No business logic in handlers; no direct Mongo/Lucene/Git access."
  (:require [reitit.ring :as reitit-ring]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [epiphany.application.commands :as commands]
            [epiphany.infra.adapters.ollama :as ollama]
            [epiphany.infra.profile :as profile]
            [epiphany.infra.workbench :as workbench]))

;; ---------------------------------------------------------------------------
;; Problem+json (RFC 9457)

(defn problem-response
  "Create an RFC 9457 problem+json response."
  [status title detail & {:keys [type instance errors]
                          :or {type "about:blank"}}]
  (response/status
   (response/content-type
    (response/response
     (json/write-str
      (cond-> {:type type
               :title title
               :status status
               :detail detail}
        instance (assoc :instance instance)
        errors (assoc :errors errors))))
    "application/problem+json")
   status))

(defn unavailable-problem
  "Create an UNAVAILABLE (503) problem response."
  [detail]
  (problem-response 503 "Service Unavailable" detail))

(defn bad-request-problem
  "Create a BAD REQUEST (400) problem response."
  [detail]
  (problem-response 400 "Bad Request" detail))

(defn not-found-problem
  "Create a NOT FOUND (404) problem response."
  [detail]
  (problem-response 404 "Not Found" detail))

(defn internal-error-problem
  "Create an INTERNAL ERROR (500) problem response."
  [detail]
  (problem-response 500 "Internal Server Error" detail))

(defn malformed-edn-problem
  "Create a BAD REQUEST (400) problem response for a body that failed to
  parse as data (ENG-017K) — reader-eval attempts, unknown tags, and
  malformed EDN/JSON all land here rather than throwing past the boundary."
  [detail]
  (problem-response 400 "Bad Request" detail :type "urn:epiphany:boundary/malformed-edn"))

;; ---------------------------------------------------------------------------
;; Content negotiation

(defn- parse-accept
  "Parse Accept header to determine response format."
  [accept-header]
  (cond
    (nil? accept-header) :json
    (.contains accept-header "application/edn") :edn
    (.contains accept-header "application/json") :json
    (.contains accept-header "text/plain") :text
    :else :json))

(defn- content-type-for
  "Get content type for format."
  [fmt]
  (case fmt
    :json "application/json"
    :edn "application/edn"
    :text "text/plain"
    "application/json"))

(defn- serialize
  "Serialize data to the specified format."
  [data fmt]
  (case fmt
    :json (json/write-str data :key-fn (fn [k] (subs (str k) 1)))
    :edn (pr-str data)
    :text (str data)
    (json/write-str data :key-fn (fn [k] (subs (str k) 1)))))

(defn- read-body
  "Read and parse request body. EDN parsing uses clojure.edn with no
  data readers — unknown tags and #=(...) are parse errors, never
  evaluations (ENG-017K)."
  [request]
  (when-let [body (:body request)]
    (let [body-str (slurp body)
          content-type (get-in request [:headers "content-type"] "")]
      (cond
        (.contains content-type "application/edn")
        (edn/read-string {:readers {}} body-str)

        (.contains content-type "application/json")
        (json/read-str body-str :key-fn keyword)

        :else
        (try (json/read-str body-str :key-fn keyword)
             (catch Exception _
               (edn/read-string {:readers {}} body-str)))))))

(defn- parse-query-params
  "Parse a query string into a keyword-keyed map. Returns nil when blank."
  [qs]
  (when-not (str/blank? qs)
    (into {}
          (for [pair (str/split qs #"&")
                :let [[k v] (str/split pair #"=" 2)]
                :when (seq k)]
            [(keyword (java.net.URLDecoder/decode k "UTF-8"))
             (java.net.URLDecoder/decode (or v "") "UTF-8")]))))

(defn- wrap-query-params
  "Middleware that parses the query string into keyword-keyed :query-params.

  Accepts the query string either embedded in :uri (as the test harness and
  some clients send it) or in the standard ring :query-string. When embedded
  in :uri, strips it so reitit routes on the path alone."
  [handler]
  (fn [request]
    (let [uri (:uri request)
          idx (.indexOf uri "?")
          [path embedded-qs] (if (neg? idx)
                               [uri nil]
                               [(subs uri 0 idx) (subs uri (inc idx))])
          qs (or embedded-qs (:query-string request))
          request (cond-> (assoc request :uri path)
                    (parse-query-params qs)
                    (update :query-params merge (parse-query-params qs)))]
      (handler request))))

(def ^:private generic-internal-error-detail
  "A server-side fault has no client-actionable detail; the real message
  goes to *err*, never to the response body (ENG-017G boundary hardening)."
  "An internal error occurred.")

(defn- log-server-error!
  [^Exception e]
  (binding [*out* *err*]
    (println "Unhandled exception in HTTP handler:" (.getMessage e))))

(defn wrap-exceptions
  "Middleware to catch exceptions and return problem+json responses.

  Recognized :code values on an ex-info carry a client-safe message by
  contract and are returned verbatim. Anything else — an unrecognized
  :code or a bare exception — is a programming/internal fault: the real
  message is logged server-side only, and the client gets a generic,
  non-leaking detail (ENG-017G)."
  [handler]
  (fn [request]
    (try
      (let [response (handler request)]
        (if (and (map? response) (:status response))
          response
          (response/response (str response))))
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (case (:code data)
            :unavailable (unavailable-problem (.getMessage e))
            :not-found (not-found-problem (.getMessage e))
            :bad-request (bad-request-problem (.getMessage e))
            (do (log-server-error! e)
                (internal-error-problem generic-internal-error-detail)))))
      (catch Exception e
        (log-server-error! e)
        (internal-error-problem generic-internal-error-detail)))))

(defn wrap-profile
  "Middleware to inject profile from query params or header."
  [handler]
  (fn [request]
    (let [profile (or (get-in request [:query-params :profile])
                      (get-in request [:headers "x-profile"])
                      "local")
          profile (keyword profile)]
      (if (profile/valid-profile? profile)
        (handler (assoc request :profile profile))
        (bad-request-problem (str "Invalid profile: " (pr-str profile)
                                  ". Valid: " (pr-str profile/valid-profiles)))))))

;; ---------------------------------------------------------------------------
;; Handlers

(def max-search-limit
  "Upper bound on :limit shared with the CLI's --limit validation
   (ENG-017G boundary hardening); guards against unbounded result sets."
  1000)

(defn valid-limit?
  [limit]
  (and (integer? limit) (pos? limit) (<= limit max-search-limit)))

;; ---------------------------------------------------------------------------
;; ENG-017G2 seam encode
;;
;; One normalized-category -> problem response mapping for every command
;; that flows decode -> execute -> encode. Handlers no longer build
;; per-command error tables.

(defn- encode-outcome
  "Encode a normalized outcome as a ring response. Accepted payloads are
   shaped by `present` before serialization; rejections/unavailable/
   not-found become RFC 9457 problems from the single category table."
  [outcome fmt present & {:keys [accepted-status] :or {accepted-status 200}}]
  (let [category (:outcome/category outcome)
        payload (:outcome/payload outcome)]
    (if (= :accepted category)
      (-> (response/response (serialize (present payload) fmt))
          (response/content-type (content-type-for fmt))
          (response/status accepted-status))
      (problem-response (commands/http-status-for-category category)
                        (commands/http-title-for-category category)
                        (:detail payload)))))

(defn- rejected->problem
  "A decode-level rejection becomes the same 400 an execute-level one does."
  [outcome]
  (problem-response 400 "Bad Request" (:detail (:outcome/payload outcome))))

(defn- parse-uuid-or-raw
  "Parse s as a UUID; return the raw value on failure so schema validation
   (not a surface-specific nil check) produces the rejection."
  [s]
  (if (string? s)
    (try (java.util.UUID/fromString s)
         (catch Exception _ s))
    s))

(defn search-handler
  "Handle search requests."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          fmt (parse-accept (get-in request [:headers "accept"]))
          candidate (cond-> {:command/name :query/search
                             :query (if (str/blank? (:query body)) "" (:query body))
                             :mode (let [m (or (:mode body) "hybrid")]
                                     (if (string? m) (keyword m) m))
                             :limit (or (:limit body) 20)}
                      (:embedding-version body)
                      (assoc :embedding-version (:embedding-version body))
                      (:path-prefix body) (assoc-in [:filters :path-prefix] (:path-prefix body))
                      (:ref body) (assoc-in [:filters :ref] (:ref body)))
          decoded (commands/decode candidate)]
      (if (commands/rejected? decoded)
        (rejected->problem decoded)
        (encode-outcome (commands/execute {:search-ports adapters
                                           :service-available? ollama/available?}
                                          decoded)
                        fmt identity)))))

(defn register-handler
  "Handle register requests."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          candidate (cond-> {:command/name :command/register
                             :repository-path (or (:path body) (:repository-path body) "")}
                      (:request-id body)
                      (assoc :request-id (parse-uuid-or-raw (:request-id body))))
          decoded (commands/decode candidate)]
      (if (commands/rejected? decoded)
        (rejected->problem decoded)
        (encode-outcome (commands/execute {:adapters adapters} decoded)
                        :json identity
                        :accepted-status 201)))))

(defn status-handler
  "Handle status requests."
  [adapters]
  (fn [request]
    (let [fmt (parse-accept (get-in request [:headers "accept"]))
          candidate {:command/name :query/status
                     :resource-id (parse-uuid-or-raw (get-in request [:path-params :resource-id]))}
          decoded (commands/decode candidate)]
      (if (commands/rejected? decoded)
        (rejected->problem decoded)
        (encode-outcome (commands/execute {:adapters adapters} decoded)
                        fmt identity)))))

(defn review-decisions-handler
  "Handle POST /api/v1/review-decisions: creates a review-decision command
   resource against a real, existing lineage candidate -- never a mutable
   update of the candidate itself (ADR/CLAUDE.md epistemic ladder: a
   decision is a durable event, not an edit). Flows through the ENG-017G2
   seam, so an HTTP-recorded decision and a CLI-recorded one execute the
   identical validated command map."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          candidate (cond-> {:command/name :command/review-decision
                             :candidate-id (parse-uuid-or-raw (:candidate-id body))
                             :decision (if (str/blank? (:decision body))
                                         ""
                                         (keyword (:decision body)))}
                      (:rationale body) (assoc :reason (:rationale body)))
          decoded (commands/decode candidate)]
      (if (commands/rejected? decoded)
        (rejected->problem decoded)
        (encode-outcome (commands/execute {:adapters adapters} decoded)
                        :json
                        (fn [{:keys [decision]}]
                          {:id (:review-decision/id decision)
                           :decision (name (:review-decision/decision decision))
                           :candidate-id (str (:review-decision/candidate-id decision))
                           :rationale (:review-decision/reason decision)
                           :created-at (:review-decision/decided-at decision)})
                        :accepted-status 201)))))

;; ---------------------------------------------------------------------------
;; Router

(defn make-router
  "Create the reitit ring handler with all workbench + API v1 routes.

  Query-string parsing and exception translation are applied as outer
  middleware; query-param stripping must run before reitit routing so that
  paths with embedded query strings (e.g. /htmx/evidence?path=..) still match."
  [adapters]
  (let [handler (reitit-ring/ring-handler
                 (reitit-ring/router
                  ["/"
                   ["" {:get {:handler (workbench/search-page-handler adapters)}}]
                   ["htmx/search"
                    {:post {:handler (workbench/search-htmx-handler adapters)}}]
                   ["htmx/evidence"
                    {:get {:handler (workbench/evidence-htmx-handler adapters)}}]
     ["htmx/evidence/empty"
      {:get {:handler (workbench/evidence-empty-handler adapters)}}]
     ["timeline"
      {:get {:handler (workbench/timeline-page-handler adapters)}}]
     ["htmx/timeline"
      {:post {:handler (workbench/timeline-htmx-handler adapters)}}]
     ["inbox"
      {:get {:handler (workbench/inbox-page-handler adapters)}}]
     ["htmx/inbox"
      {:post {:handler (workbench/inbox-htmx-handler adapters)}}]
     ["htmx/inbox/decide"
      {:post {:handler (workbench/inbox-decide-htmx-handler adapters)}}]
     ["health"
      {:get {:handler (workbench/health-page-handler adapters)}}]
     ["htmx/health"
      {:post {:handler (workbench/health-htmx-handler adapters)}}]
     ["api/v1/search"
                    {:post {:handler (search-handler adapters)}}]
                   ["api/v1/register"
                    {:post {:handler (register-handler adapters)}}]
                   ["api/v1/status/:resource-id"
                    {:get {:handler (status-handler adapters)}}]
                   ["api/v1/review-decisions"
                    {:post {:handler (review-decisions-handler adapters)}}]])
                 (reitit-ring/routes
                  (reitit-ring/create-resource-handler {:path "/static"})
                  (reitit-ring/create-default-handler
                   {:not-found (fn [_] (not-found-problem "Route not found"))})))]
    (-> handler
        wrap-exceptions
        wrap-query-params)))

(defn create-handler
  "Create the complete handler with middleware.

  A request body that fails to parse as data — a reader-eval attempt, an
  unknown tag, or malformed EDN/JSON — is rejected here as a stable
  :boundary/malformed-edn problem response (ENG-017K); it never reaches a
  route handler and never throws past this boundary."
  [adapters]
  (let [router (make-router adapters)]
    (fn [request]
      (try
        (let [body-params (or (:body-params request)
                              (when (:body request) (read-body request)))
              request (cond-> request
                        body-params (assoc :body-params body-params)
                        (not (:path-params request)) (assoc :path-params {}))]
          ((wrap-profile router) request))
        (catch Exception e
          (malformed-edn-problem (.getMessage e)))))))

;; ---------------------------------------------------------------------------
;; Server

(defn start-server!
  "Start the HTTP server on the specified port.
   Returns the server instance."
  [adapters port]
  (let [handler (create-handler adapters)]
    (jetty/run-jetty handler {:port port :join? false})))

(defn stop-server!
  "Stop the HTTP server."
  [server]
  (.stop server))
