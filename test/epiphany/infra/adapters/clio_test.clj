(ns epiphany.infra.adapters.clio-test
  (:require [clio.extern.jvm.fs :as fs]
            [clio.infra.runtime :as runtime]
            [clojure.test :refer [deftest is testing]]
            [epiphany.extern.clio-observations :as host]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.profile :as profile]
            [epiphany.law-suite.observations-laws :as laws]))

(defn- temporary-directory []
  (str (System/getProperty "java.io.tmpdir") "/epiphany-clio-" (random-uuid)))

(defn- location [request-id]
  (assoc ((get-in laws/op-fixtures [:record-repository-location! :make-valid]) request-id)
         :repository/path {:path/raw "/repo/.ημ/notes.md"
                           :path/source :filesystem-argument :path/comparison :exact}))

(defn- error-data [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo error (ex-data error))))

(deftest all-observation-laws-run-without-skips
  (let [directories (atom [])]
    (try
      (let [outcomes (laws/observations-laws
                      {:make-port (fn []
                                    (let [directory (temporary-directory)]
                                      (swap! directories conj directory)
                                      (clio/make-observations-adapter
                                       (clio/open-store directory))))
                       :capabilities #{:schema-validation :idempotency :export-import}})]
        (is (seq outcomes))
        (is (= #{} (laws/failed-laws outcomes)) (pr-str outcomes))
        (is (= #{} (laws/skipped-laws outcomes))))
      (finally (doseq [directory @directories] (fs/remove-tree! directory))))))

(deftest restart-retries-and-clear-preserve-canonical-history
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            request-id (random-uuid)
            record (location request-id)]
        (is (nil? ((:record-repository-location! port) record)))
        (let [first-bytes (fs/read-text (:file store))
              restarted (clio/make-observations-adapter (clio/open-store directory))]
          (is (= record ((:find-by-request-id restarted) request-id)))
          (is (nil? ((:record-repository-location! restarted) record)))
          (is (= :idempotency-conflict
                 (:code ((:record-repository-location! restarted)
                         (assoc record :observation/id (random-uuid))))))
          (is (= first-bytes (fs/read-text (:file store))))
          (is (= :schema-validation-failed
                 (:code (error-data #((:record-ingestion-run! restarted) {})))))
          (is (= first-bytes (fs/read-text (:file store))))
          (let [exported ((:export-all restarted))]
            ((:clear-all! restarted))
            (is (= 2 (count (clio/history store))))
            (is (nil? ((:find-by-request-id
                        (clio/make-observations-adapter (clio/open-store directory))) request-id)))
            ((:import-all restarted) exported)
            (is (= record ((:find-by-request-id
                            (clio/make-observations-adapter (clio/open-store directory))) request-id)))
            (is (= 3 (count (clio/history store)))))))
      (finally (fs/remove-tree! directory)))))

(deftest independent-handles-do-not-lose-concurrent-writes
  (let [directory (temporary-directory)]
    (try
      (let [ports (repeatedly 4 #(clio/make-observations-adapter (clio/open-store directory)))
            records (repeatedly 4 #(location (random-uuid)))
            writes (mapv (fn [port record]
                           (future ((:record-repository-location! port) record))) ports records)]
        (doseq [write writes] (is (nil? (deref write 10000 ::timeout))))
        (is (= (set records) (set (clio/list-repository-locations (clio/open-store directory))))))
      (finally (fs/remove-tree! directory)))))

(deftest missing-or-corrupt-history-never-becomes-an-empty-store
  (doseq [failure [:ledger :schema :corrupt]]
    (let [directory (temporary-directory)]
      (try
        (let [store (clio/open-store directory)
              port (clio/make-observations-adapter store)]
          ((:record-repository-location! port) (location (random-uuid)))
          (case failure
            :ledger (fs/delete-if-exists! (:file store))
            :schema (fs/remove-tree! (:schemas store))
            :corrupt (fs/write-text! (:file store) "{:one 1} {:two 2}\n"))
          (testing (name failure)
            (is (some? (error-data #(clio/open-store directory)))))
          (when (= :ledger failure) (is (not (fs/exists? (:file store)))))
          (when (= :schema failure) (is (not (fs/exists? (:schemas store))))))
        (finally (fs/remove-tree! directory))))))

(deftest profile-composes-durable-observations-without-mongo
  (let [directory (temporary-directory)]
    (try
      (let [options {:profile :edn :edn-dir (str directory "/ledger")
                     :index-dir (str directory "/index") :common-git-dir-fn identity}
            record (location (random-uuid))
            port (:observations (profile/resolve-adapters options))]
        (is (profile/valid-profile? :edn))
        ((:record-repository-location! port) record)
        (is (= record ((get-in (profile/resolve-adapters options)
                               [:observations :find-by-request-id])
                       (:observation/request-id record))))
        (is (= :unavailable
               (:code (error-data #(profile/resolve-adapters (dissoc options :index-dir)))))))
      (finally (fs/remove-tree! directory)))))

(deftest failed-append-does-not-publish-staged-memory
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            record (location (random-uuid))]
        (with-redefs [runtime/append! (fn [& _]
                                        (throw (ex-info "Injected disk refusal"
                                                        {:code :disk-refused})))]
          (is (= :disk-refused
                 (:code (error-data #((:record-repository-location! port) record))))))
        (is (empty? (clio/history store)))
        (is (nil? ((:find-by-request-id port) (:observation/request-id record)))))
      (finally (fs/remove-tree! directory)))))

(deftest semantically-invalid-accepted-event-is-refused-on-restart
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)]
        ;; The Clio envelope is valid, but clearing an already empty projection
        ;; cannot truthfully be an accepted state-changing operation.
        (runtime/append!
         (:runtime store) (:file store) :epiphany.observations/operation-accepted
         {:event/stream "epiphany/observations" :event/seq 1 :event/causes []
          :event/actor "literal-test" :event/subject "epiphany/observations"
          :event/data {:operation :clear-all! :arguments [] :result nil
                       :before-hash (host/state-hash {}) :after-hash (host/state-hash {})}})
        (is (= :integrity/replay-conflict
               (:code (error-data #(clio/open-store directory))))))
      (finally (fs/remove-tree! directory)))))

(deftest altered-state-digest-is-refused-even-when-the-command-succeeds
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)]
        ((:record-repository-location! port) (location (random-uuid)))
        (let [event (first (clio/history store))
              forged (assoc-in event [:event/data :after-hash] (apply str (repeat 64 "0")))]
          (fs/write-text! (:file store) (str (pr-str forged) "\n"))
          (is (= :integrity/replay-conflict
                 (:code (error-data #(clio/open-store directory)))))))
      (finally (fs/remove-tree! directory)))))
