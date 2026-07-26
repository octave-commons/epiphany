(ns epiphany.application.commands
  "Shared CLI/HTTP command-vocabulary seam (ENG-017G2, ADR-004 decision 5).

  Both surfaces run the same pipeline:

      decode (surface raw input -> validated command map)
      execute (command map + ports -> normalized outcome)
      encode (outcome -> surface-specific response)

  Parity is structural: equivalent input produces the identical command
  map on both surfaces, the same executor runs it, and only the terminal
  encoding differs (exit codes + text vs RFC 9457 problem+json).

  Outcome categories (the one normalized set):
    :accepted      — the command ran; payload is the result
    :rejected      — client input failed validation (400 / exit 1)
    :unavailable   — a required service is unreachable (503 / exit 1)
    :not-found     — the named resource does not exist (404 / exit 1)

  No I/O in decode or encode-cli; execute performs the side effects
  through injected ports."
  (:require [epiphany.application.registration :as registration]
            [epiphany.domain.hybrid-search :as hs]
            [epiphany.domain.review :as review]
            [epiphany.domain.status :as status]
            [epiphany.law.registry :as registry]))

;; ---------------------------------------------------------------------------
;; Outcomes

(defn- outcome
  [category payload]
  {:outcome/category category
   :outcome/payload payload})

(defn accepted [result] (outcome :accepted result))
(defn rejected [detail] (outcome :rejected {:detail detail}))
(defn unavailable [detail] (outcome :unavailable {:detail detail}))
(defn not-found [detail] (outcome :not-found {:detail detail}))

;; ---------------------------------------------------------------------------
;; Decode
;;
;; Each surface translates its raw input into a CANDIDATE command map;
;; `decode` validates it against the named law/ schema. Invalid input is
;; a stable :rejected outcome — an adapter is never reached directly.

(def schema-for-command
  {:command/register "command/register"
   :query/search "query/search"
   :query/status "query/status"
   :command/review-decision "command/review-decision"})

(defn decode
  "Validate a candidate command map against its law/ schema. Returns the
   command map on success, a :rejected outcome map on failure."
  [candidate]
  (let [command-name (:command/name candidate)
        schema-name (get schema-for-command command-name)]
    (cond
      (nil? schema-name)
      (rejected (str "Unknown command: " (pr-str command-name)))

      :else
      (if-let [explanation (registry/explain schema-name candidate)]
        (rejected (str "Invalid " (name command-name) " command: "
                       (pr-str (mapv #(select-keys % [:path :message])
                                     (:errors explanation)))))
        candidate))))

(defn rejected?
  [x]
  (= :rejected (:outcome/category x)))

;; ---------------------------------------------------------------------------
;; Execute
;;
;; ctx carries whatever ports the command needs:
;;   :adapters     — full application port map (register, status, review)
;;   :search-ports — {:index ... :embeddings ...} for query/search

(defn- execute-register
  [ctx command]
  (try
    (accepted (registration/register! (:adapters ctx)
                                      (cond-> {:repository-path (:repository-path command)}
                                        (:request-id command)
                                        (assoc :request-id (:request-id command)))))
    (catch clojure.lang.ExceptionInfo e
      (if (= :unavailable (:code (ex-data e)))
        (unavailable (.getMessage e))
        (rejected (.getMessage e))))))

(defn- execute-search
  [ctx command]
  (if (and (#{:semantic :hybrid} (:mode command))
           (:service-available? ctx)
           (not ((:service-available? ctx))))
    (unavailable "Semantic/hybrid search requires the local Ollama service.")
    (try
      (accepted (hs/search (:search-ports ctx) (dissoc command :command/name)))
      (catch clojure.lang.ExceptionInfo e
        (if (= :unavailable (:code (ex-data e)))
          (unavailable (.getMessage e))
          (rejected (.getMessage e)))))))

(defn- execute-status
  [ctx command]
  (accepted (status/query-status (:adapters ctx) (:resource-id command))))

(def ^:private decision-payload-keys
  [:review-decision/candidate-id
   :review-decision/decision
   :review-decision/reason
   :review-decision/relabel-to
   :review-decision/annotation])

(defn- observation->decision
  [observation]
  (cond-> (select-keys observation
                       [:review-decision/id
                        :review-decision/candidate-id
                        :review-decision/decision
                        :review-decision/decided-at
                        :review-decision/reason
                        :review-decision/relabel-to
                        :review-decision/annotation
                        :review-decision/suppressed])
    (:observation/request-id observation)
    (assoc :review-decision/request-id (:observation/request-id observation))))

(defn- command->decision-payload
  [command]
  (cond-> {:review-decision/candidate-id (:candidate-id command)
           :review-decision/decision (:decision command)}
    (:reason command)
    (assoc :review-decision/reason (:reason command))
    (:relabel-to command)
    (assoc :review-decision/relabel-to (:relabel-to command))
    (:annotation command)
    (assoc :review-decision/annotation (:annotation command))))

(defn- execute-review-decision
  [ctx command]
  (let [observations (:observations (:adapters ctx))
        candidate-id (:candidate-id command)
        candidate ((:find-lineage-candidate-by-id observations) candidate-id)]
    (if-not candidate
      (not-found (str "No lineage candidate found for id " candidate-id))
      (let [list-decisions (:list-review-decisions-by-candidate observations)
            existing (when list-decisions
                       (some #(when (= (:request-id command)
                                       (:observation/request-id %))
                                %)
                             (list-decisions candidate-id)))]
        (if existing
          (let [persisted (observation->decision existing)]
            (if (= (command->decision-payload command)
                   (select-keys persisted decision-payload-keys))
              (accepted {:decision persisted :candidate candidate})
              (rejected (str "Idempotency conflict for review request "
                             (:request-id command)))))
          (let [decision (review/make-decision candidate-id (:decision command)
                                               :request-id (:request-id command)
                                               :reason (:reason command)
                                               :relabel-to (:relabel-to command)
                                               :annotation (:annotation command))
                observation (review/decision->observation
                             decision {:resource-id (:resource-id candidate)
                                       :adapter-version "0.1.0"})]
            ((:record-review-decision! observations) observation)
            (if-not list-decisions
              ;; Compatibility for narrow test doubles and legacy adapters.
              ;; Production adapters expose the read-after-write port below.
              (accepted {:decision decision :candidate candidate})
              (let [persisted-observation
                    (some #(when (= (:request-id command)
                                    (:observation/request-id %))
                             %)
                          (list-decisions candidate-id))
                    persisted (when persisted-observation
                                (observation->decision persisted-observation))]
                (cond
                  (nil? persisted)
                  (unavailable (str "Review decision was not readable after write for request "
                                    (:request-id command)))

                  (= (command->decision-payload command)
                     (select-keys persisted decision-payload-keys))
                  (accepted {:decision persisted :candidate candidate})

                  :else
                  (rejected (str "Idempotency conflict for review request "
                                 (:request-id command))))))))))))

(defn execute
  "Execute a validated command map. Returns a normalized outcome map."
  [ctx command]
  (case (:command/name command)
    :command/register (execute-register ctx command)
    :query/search (execute-search ctx command)
    :query/status (execute-status ctx command)
    :command/review-decision (execute-review-decision ctx command)
    (rejected (str "No executor for command: " (pr-str (:command/name command))))))

;; ---------------------------------------------------------------------------
;; HTTP encode
;;
;; One normalized-category -> status-code mapping; handlers stop building
;; their own parallel tables. Response bodies stay plain data here — the
;; infra/http layer serializes.

(def http-status-for-category
  {:accepted 200
   :rejected 400
   :unavailable 503
   :not-found 404})

(def http-title-for-category
  {:accepted "OK"
   :rejected "Bad Request"
   :unavailable "Service Unavailable"
   :not-found "Not Found"})
