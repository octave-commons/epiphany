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
            [epiphany.application.registration :as registration]
            [epiphany.domain.hybrid-search :as hs]
            [epiphany.domain.status :as status]
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

(defn search-handler
  "Handle search requests."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          query (:query body)
          mode (or (:mode body) "hybrid")
          mode (if (string? mode) (keyword mode) mode)
          limit (or (:limit body) 20)
          path-prefix (:path-prefix body)
          ref (:ref body)
          fmt (parse-accept (get-in request [:headers "accept"]))]
      (cond
        (str/blank? query)
        (bad-request-problem "Query is required")

        (not (#{:lexical :semantic :hybrid} mode))
        (bad-request-problem (str "Invalid mode: " (pr-str mode)
                                  ". Must be lexical, semantic, or hybrid"))

        (not (valid-limit? limit))
        (bad-request-problem (str "Invalid limit: " (pr-str limit)
                                  ". Must be a positive integer <= " max-search-limit))

        :else
        (let [request-map (cond-> {:query query
                                   :mode mode
                                   :limit limit}
                            path-prefix (assoc-in [:filters :path-prefix] path-prefix)
                            ref (assoc-in [:filters :ref] ref))
              results (hs/search adapters request-map)
              body (serialize results fmt)]
          (-> (response/response body)
              (response/content-type (content-type-for fmt))
              (response/status 200)))))))

(defn register-handler
  "Handle register requests."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          path (or (:path body) (:repository-path body))]
      (cond
        (str/blank? path)
        (bad-request-problem "Path is required")

        :else
        (try
          (let [result (registration/register! adapters {:repository-path path})]
            (-> (response/response (serialize result :json))
                (response/content-type "application/json")
                (response/status 201)))
          (catch clojure.lang.ExceptionInfo e
            (let [data (ex-data e)]
              (case (:code data)
                :unavailable (unavailable-problem (.getMessage e))
                :already-exists (problem-response 409 "Conflict" (.getMessage e))
                (problem-response 400 "Bad Request"
                                  (str (.getMessage e)
                                       (when (:explanation data)
                                         (str "\n" (pr-str (:explanation data))))))))))))))

(defn status-handler
  "Handle status requests."
  [adapters]
  (fn [request]
    (let [resource-id-str (get-in request [:path-params :resource-id])
          resource-id (try
                        (java.util.UUID/fromString resource-id-str)
                        (catch Exception _ nil))]
      (cond
        (nil? resource-id)
        (bad-request-problem "Invalid resource ID format")

        :else
        (let [result (status/query-status adapters resource-id)
              fmt (parse-accept (get-in request [:headers "accept"]))
              body (serialize result fmt)]
          (-> (response/response body)
              (response/content-type (content-type-for fmt))
              (response/status 200)))))))

(defn review-decisions-handler
  "Handle review decision creation."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          decision (:decision body)
          candidate-id (:candidate-id body)
          rationale (:rationale body)]
      (cond
        (str/blank? decision)
        (bad-request-problem "Decision is required")

        (str/blank? candidate-id)
        (bad-request-problem "Candidate ID is required")

        :else
        (let [result {:id (java.util.UUID/randomUUID)
                      :decision decision
                      :candidate-id candidate-id
                      :rationale rationale
                      :created-at (java.util.Date.)}]
          (-> (response/response (serialize result :json))
              (response/content-type "application/json")
              (response/status 201)))))))

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
