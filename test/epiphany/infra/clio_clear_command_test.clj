(ns epiphany.infra.clio-clear-command-test
  (:require [clio.extern.jvm.fs :as fs]
            [clio.infra.runtime :as runtime]
            [clio.infra.schema-store :as schema-store]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [epiphany.domain.backup :as backup]
            [epiphany.extern.clio-observations :as host]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.adapters.in-memory :as memory]
            [epiphany.law-suite.observations-laws :as laws]))

(defn- temporary-directory []
  (str (System/getProperty "java.io.tmpdir") "/epiphany-clear-command-" (random-uuid)))

(defn- location []
  ((get-in laws/op-fixtures [:record-repository-location! :make-valid]) (random-uuid)))

(defn- error-code [operation]
  (try (operation) nil (catch clojure.lang.ExceptionInfo cause (:code (ex-data cause)))))

(deftest identified-clear-retry-preserves-intervening-writes
  (doseq [initially-empty? [false true]]
    (let [directory (temporary-directory)]
      (try
        (let [store (clio/open-store directory)
              port (clio/make-observations-adapter store)
              command-id (random-uuid)
              later-record (location)]
          (when-not initially-empty? ((:record-repository-location! port) (location)))
          (is (nil? ((:clear-all! port) command-id)))
          (is (= command-id (get-in (last (clio/history store)) [:event/data :command-id])))
          ((:record-repository-location! port) later-record)
          (let [reopened (clio/make-observations-adapter (clio/open-store directory))
                bytes (fs/read-text (:file store))]
            (is (nil? ((:clear-all! reopened) command-id)))
            (is (= later-record ((:find-by-request-id reopened) (:observation/request-id later-record))))
            (is (= bytes (fs/read-text (:file store))))
            (doseq [arguments [[] [nil] ["not-a-uuid"] [command-id {:different "material"}]]]
              (is (= :invalid-observation-invocation
                     (error-code #(apply (:clear-all! reopened) arguments)))))
            (is (= bytes (fs/read-text (:file store))))
            (is (nil? ((:clear-all! reopened) (random-uuid))))
            (is (nil? ((:find-by-request-id reopened) (:observation/request-id later-record))))
            (is (= (if initially-empty? 3 4) (count (clio/history (clio/open-store directory)))))))
        (finally (fs/remove-tree! directory))))))

(deftest identified-empty-clear-and-its-retry-must-be-durable
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            command-id (random-uuid)
            fail-force (fn [_] (throw (ex-info "Injected clear fence failure" {:fault :force})))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Injected clear fence failure"
                              (with-redefs [fs/force-file! fail-force] ((:clear-all! port) command-id))))
        (is (= 1 (count (clio/history store))))
        (let [bytes (fs/read-text (:file store))]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Injected clear fence failure"
                                (with-redefs [fs/force-file! fail-force] ((:clear-all! port) command-id))))
          (is (= bytes (fs/read-text (:file store))))
          (is (nil? ((:clear-all! port) command-id)))
          (is (= bytes (fs/read-text (:file store))))))
      (finally (fs/remove-tree! directory)))))

(deftest historical-unidentified-clear-retains-its-original-replay-contract
  (let [directory (temporary-directory)]
    (try
      (let [schemas (str directory "/schemas")
            _ (fs/ensure-dir! directory)
            file (host/ensure-ledger! directory)
            old-catalog (edn/read-string (slurp (io/resource "fixtures/clio/observations-pre-command-catalog.edn")))
            old-runtime (runtime/open schemas old-catalog)
            old-schema-file (schema-store/revision-path schemas (get-in old-runtime [:schema/current :schema/root]))
            old-schema-bytes (fs/read-text old-schema-file)
            store {:directory directory :file file :schemas schemas :runtime old-runtime}
            port (clio/make-observations-adapter store)
            reference (:observations (memory/make {:common-git-dir-fn identity}))]
        ((:record-repository-location! port) (location))
        (runtime/append!
         (:runtime store) (:file store) :epiphany.observations/operation-accepted
         {:event/stream "epiphany/observations" :event/seq 2
          :event/causes [(:event/id (first (clio/history store)))]
          :event/actor "historical-fixture" :event/subject "epiphany/observations"
          :event/data {:operation :clear-all! :arguments [] :result nil
                       :before-hash (host/state-hash ((:export-all port)))
                       :after-hash (host/state-hash ((:export-all reference)))}})
        (let [bytes (fs/read-text (:file store))
              reopened (clio/make-observations-adapter (clio/open-store directory))]
          (is (= ((:export-all reference)) ((:export-all reopened))))
          (is (= bytes (fs/read-text (:file store))))
          (is (= 2 (count (clio/history store))))
          (is (= old-schema-bytes (fs/read-text old-schema-file)))
          (is (= #{(get-in old-runtime [:schema/current :schema/root])}
                 (set (map #(get-in % [:event/schema :schema/root]) (clio/history store)))))
          (is (= 2 (count (schema-store/load-revisions schemas))))))
      (finally (fs/remove-tree! directory)))))

(deftest forged-duplicate-clear-identity-refuses-replay
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            command-id (random-uuid)]
        ((:clear-all! port) command-id)
        (let [accepted (first (clio/history store))]
          (runtime/append!
           (:runtime store) (:file store) :epiphany.observations/operation-accepted
           {:event/stream "epiphany/observations" :event/seq 2 :event/causes [(:event/id accepted)]
            :event/actor "corruption-fixture" :event/subject "epiphany/observations"
            :event/data (:event/data accepted)}))
        (is (= :integrity/duplicate-command (error-code #(clio/open-store directory)))))
      (finally (fs/remove-tree! directory)))))

(deftest restore-drill-passes-the-explicit-durable-clear-command
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store (str directory "/store"))
            port (clio/make-observations-adapter store)
            record (location)
            command-id (random-uuid)]
        ((:record-repository-location! port) record)
        (let [report (backup/restore-drill port {:common-git-directory (constantly directory)}
                                           (str directory "/backup") command-id)]
          (is (= :complete (:drill-status report)))
          (is (:round-trip-identical? report))
          (is (= command-id (get-in (second (clio/history store)) [:event/data :command-id])))
          (is (= record ((:find-by-request-id port) (:observation/request-id record))))))
      (finally (fs/remove-tree! directory)))))
