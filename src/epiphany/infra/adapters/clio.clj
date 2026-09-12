(ns epiphany.infra.adapters.clio
  "Durable Clio operations projected through the existing observation reference.

  The reference is disposable, never a fallback. Every read rebuilds from
  canonical events. No Mongo query language or destructive ledger edit is used."
  (:require [clio.infra.ledger :as ledger]
            [clio.infra.runtime :as runtime]
            [clio.infra.schema-store :as schema-store]
            [epiphany.extern.clio-observations :as host]
            [epiphany.infra.adapters.in-memory :as memory]
            [epiphany.law.clio-observations :as law]))

(defn- reference-port []
  (:observations (memory/make {:common-git-dir-fn identity})))

(defn history
  "Return validated causal history, refusing deleted files and historical schemas."
  [{:keys [file schemas]}]
  (:canonical/events
   (ledger/canonicalize-files (schema-store/load-revisions schemas) [file])))

(defn- replay
  [events]
  (let [port (reference-port)]
    (doseq [event events]
      (let [{:keys [operation arguments result before-hash after-hash]} (:event/data event)
            before ((:export-all port))]
        (when-not (and (= "epiphany/observations" (:event/stream event))
                       (= :epiphany.observations/operation-accepted (:event/type event)))
          (throw (ex-info "Foreign event in observation history"
                          {:code :integrity/foreign-event :event/id (:event/id event)})))
        (law/assert-arguments! operation arguments)
        (let [actual (apply (get port operation) arguments)]
          (when (or (not= actual result) (= before ((:export-all port)))
                    (not= before-hash (host/state-hash before))
                    (not= after-hash (host/state-hash ((:export-all port)))))
            (throw (ex-info "Accepted observation operation cannot be replayed"
                            {:code :integrity/replay-conflict
                             :event/id (:event/id event) :operation operation}))))))
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
          (assoc store :runtime (runtime/open schemas law/catalog)))))))

(defn- invoke-read
  [store operation arguments]
  (host/with-lock!
    (:directory store)
    #(apply (get (replay (history store)) operation) arguments)))

(defn- invoke-write
  [store operation arguments]
  (law/assert-arguments! operation arguments)
  (host/with-lock!
    (:directory store)
    (fn []
      (let [events (history store)
            port (replay events)
            before ((:export-all port))
            result (apply (get port operation) arguments)
            after ((:export-all port))]
        (when (not= before after)
          (when (some? result)
            (throw (ex-info "A refused observation command changed staged state"
                            {:code :integrity/invalid-reference :operation operation})))
          (let [previous (last events)]
            (runtime/append!
             (:runtime store) (:file store) :epiphany.observations/operation-accepted
             {:event/stream "epiphany/observations"
              :event/seq (inc (count events))
              :event/causes (if previous [(:event/id previous)] [])
              :event/actor "epiphany/observations-port"
              :event/subject "epiphany/observations"
              :event/data {:operation operation :arguments arguments :result result
                           :before-hash (host/state-hash before)
                           :after-hash (host/state-hash after)}})))
        result))))

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
