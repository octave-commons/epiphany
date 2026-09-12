(ns epiphany.infra.adapters.clio
  "Durable Clio operations projected through the existing observation reference.

  The reference is disposable, never a fallback. Every read rebuilds from
  canonical events. No Mongo query language or destructive ledger edit is used."
  (:require [clio.infra.runtime :as runtime]
            [epiphany.application.validation :as validation]
            [epiphany.domain.backup :as backup]
            [epiphany.domain.observation-admission :as admission]
            [epiphany.extern.clio-observations :as host]
            [epiphany.infra.adapters.in-memory :as memory]
            [epiphany.law.clio-observations :as law]
            [epiphany.domain.observation-invocation :as invocation]))

(defn- reference-port []
  (:observations (memory/make {:common-git-dir-fn identity})))

(defn history
  "Return validated causal history, refusing deleted files and historical schemas."
  [{:keys [file schemas]}]
  (:canonical/events
   (runtime/canonicalize-files {:schema/directory schemas} [file])))

(defn- replay
  [events]
  (let [port (reference-port)]
    (loop [remaining events accepted-commands #{}]
      (when-let [event (first remaining)]
        (let [{:keys [operation arguments result before-hash after-hash command-id]} (:event/data event)
              before ((:export-all port))]
          (when-not (and (= "epiphany/observations" (:event/stream event))
                         (= :epiphany.observations/operation-accepted (:event/type event)))
            (throw (ex-info "Foreign event in observation history"
                            {:code :integrity/foreign-event :event/id (:event/id event)})))
          (law/assert-arguments! operation arguments)
          (law/assert-command! operation command-id)
          (when (and command-id (contains? accepted-commands command-id))
            (throw (ex-info "Duplicate accepted observation command identity"
                            {:code :integrity/duplicate-command :command-id command-id})))
          (let [actual (apply (get port operation) arguments)]
            (when (or (not= actual result) (and (nil? command-id) (= before ((:export-all port))))
                      (not= before-hash (host/state-hash before))
                      (not= after-hash (host/state-hash ((:export-all port)))))
              (throw (ex-info "Accepted observation operation cannot be replayed"
                              {:code :integrity/replay-conflict
                               :event/id (:event/id event) :operation operation}))))
          (recur (next remaining) (cond-> accepted-commands command-id (conj command-id))))))
    port))

(defn open-store
  "Open an explicit directory and verify its complete history before answering."
  [directory]
  (let [directory (host/directory-path directory)]
    (host/with-lock!
      directory
      (fn []
        (let [file (host/ensure-ledger! directory)
              schemas (str directory "/schemas")
              store {:directory directory :file file :schemas schemas}]
          ;; Validate before runtime/open can materialize the current schema.
          ;; A deleted historical snapshot is corruption, even when it happens
          ;; to equal today's catalog and could otherwise be regenerated.
          (replay (history store))
          ;; A failed force can leave a fully valid, visible append behind.
          ;; Reopening acknowledges durability only after fencing those bytes.
          (runtime/ensure-durable! {:schema/directory schemas} file)
          (assoc store :runtime (runtime/open schemas law/catalog)))))))

(defn- invoke-read
  [store operation arguments]
  (host/with-lock!
    (:directory store)
    #(apply (get (replay (history store)) operation) arguments)))

(defn- invoke-write
  [store operation arguments]
  (let [{:keys [arguments command-id]} (invocation/write-invocation operation arguments)]
  ;; Validate even a duplicate before admission can remove it. Historical replay
  ;; still executes the original recorded arguments through the old reference.
    (if (= operation :import-all)
      (doseq [[collection records] (first arguments)
              record records]
        (backup/validate-record collection record))
      (when (contains? admission/record-collections operation)
        ((validation/wrap-write operation (constantly nil)) (first arguments))))
    (host/with-lock!
      (:directory store)
      (fn []
        (let [events (history store)
              port (replay events)
              before ((:export-all port))
              command-status (admission/command-status events operation arguments command-id)
              arguments (when-not (= :accepted command-status)
                          (admission/invocation-arguments before operation arguments))
              result (when arguments (apply (get port operation) arguments))
              after ((:export-all port))]
          (if (and (= before after) (not= :new command-status))
          ;; A previous append may be visible after its force failed. A logical
          ;; no-op still needs a durability fence while the operation lock is held.
            (runtime/ensure-durable! (:runtime store) (:file store))
            (let [previous (last events)]
              (when (some? result)
                (throw (ex-info "A refused observation command changed staged state"
                                {:code :integrity/invalid-reference :operation operation})))
              (runtime/append!
               (:runtime store) (:file store) :epiphany.observations/operation-accepted
               {:event/stream "epiphany/observations"
                :event/seq (inc (count events))
                :event/causes (if previous [(:event/id previous)] [])
                :event/actor "epiphany/observations-port"
                :event/subject "epiphany/observations"
                :event/data (cond-> {:operation operation :arguments arguments :result result
                                     :before-hash (host/state-hash before)
                                     :after-hash (host/state-hash after)}
                              command-id (assoc :command-id command-id))})))
          (admission/write-result operation (not= before after) result))))))

(defn make-observations-adapter
  "Expose every existing observation-port method through durable Clio replay."
  [store]
  (into {}
        (map (fn [operation]
               [operation
                (fn [& arguments]
                  ((if (contains? law/write-operations operation)
                     invoke-write invoke-read)
                   store operation (vec arguments)))]))
        (keys (reference-port))))

(defn list-repository-locations
  "List exact persisted repository observations for the metadata port."
  [store]
  (get (invoke-read store :export-all []) "repository-location"))
