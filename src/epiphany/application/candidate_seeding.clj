(ns epiphany.application.candidate-seeding
  "Persist identified provisional candidates and return the accepted identity."
  (:require [epiphany.law.registry :as registry]))

(def ^:private intent-keys
  [:resource-id :lineage-candidate/relation :lineage-candidate/source
   :lineage-candidate/target :lineage-candidate/confidence
   :lineage-candidate/generator-version :lineage-candidate/tier])

(defn- matching-request [observations observation]
  (some #(when (= (:observation/request-id observation)
                  (:observation/request-id %)) %)
        ((:list-lineage-candidates observations) (:resource-id observation))))

(defn- checked-existing [expected existing]
  (when-not (= (select-keys expected intent-keys) (select-keys existing intent-keys))
    (throw (ex-info "Candidate request-id was already used for different content"
                    {:code :bad-request :reason :idempotency-conflict})))
  existing)

(defn record!
  "Return the durably stored candidate on first write or an identical retry.
   A changed intent under the same request ID is refused before another write."
  [observations observation]
  (when-not (registry/valid? "observation/lineage-candidate-v1" observation)
    (throw (ex-info "Invalid candidate observation; UUID request-id is required"
                    {:code :bad-request})))
  (if-let [existing (matching-request observations observation)]
    (let [stored (checked-existing observation existing)]
      ;; A visible fact may follow an uncertain force. The adapter's no-op
      ;; write fences those bytes before this retry acknowledges success.
      ((:record-lineage-candidate! observations) stored)
      stored)
    (do
      ((:record-lineage-candidate! observations) observation)
      (if-let [stored (matching-request observations observation)]
        (checked-existing observation stored)
        (throw (ex-info "Candidate was not readable after its write"
                        {:code :unavailable}))))))
