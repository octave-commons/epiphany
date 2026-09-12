(ns epiphany.infra.adapters.clio-runtime-ordering-test
  (:require [clio.extern.jvm.fs :as fs]
            [clio.infra.ledger :as ledger]
            [clio.law.schema :as schema]
            [clojure.test :as test]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.law.clio-observations :as law]
            [epiphany.law-suite.observations-laws :as laws]))

(defn- temporary-directory []
  (str (System/getProperty "java.io.tmpdir") "/epiphany-clio-runtime-" (random-uuid)))

(defn- location []
  ((get-in laws/op-fixtures [:record-repository-location! :make-valid]) (random-uuid)))

(defn- attempt [f]
  (try {:value (f)}
       (catch clojure.lang.ExceptionInfo error {:error (ex-data error)})))

(test/deftest a-published-schema-is-loaded-after-the-ledger-snapshot
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            record (location)
            read-ledgers ledger/read-ledgers
            published? (atom false)
            evolved-catalog (assoc law/catalog ::additional-event
                                   (schema/event-schema ::additional-event [:map]))
            outcome
            (with-redefs [ledger/read-ledgers
                          (fn [paths]
                            ;; A real second adapter publishes a new schema and
                            ;; accepted record before this reader captures bytes.
                            (when (compare-and-set! published? false true)
                              (with-redefs [law/catalog evolved-catalog]
                                (let [port (clio/make-observations-adapter
                                            (clio/open-store directory))]
                                  ((:record-repository-location! port) record))))
                            (read-ledgers paths))]
              (attempt #(clio/history store)))]
        (test/is @published?)
        (test/is (nil? (:error outcome)) (pr-str outcome))
        (test/is (= [record] (mapv #(get-in % [:event/data :arguments 0]) (:value outcome))))
        (test/is (= record ((:find-by-request-id
                             (clio/make-observations-adapter (clio/open-store directory)))
                            (:observation/request-id record)))))
      (finally (fs/remove-tree! directory)))))

(defn- with-force-refusal [phase f]
  (let [force-file! fs/force-file!
        sync-directory! fs/sync-directory!
        inode-seen? (atom false)]
    (with-redefs [fs/force-file!
                  (fn [channel]
                    (reset! inode-seen? true)
                    (if (= :inode phase)
                      (throw (ex-info "Injected ledger inode force refusal" {:code :force-refused}))
                      (force-file! channel)))
                  fs/sync-directory!
                  (fn [path]
                    (if (and @inode-seen? (= :parent phase))
                      (throw (ex-info "Injected ledger parent force refusal" {:code :force-refused}))
                      (sync-directory! path)))]
      (f))))

(test/deftest reopen-must-not-acknowledge-an-unflushed-visible-append
  (doseq [phase [:inode :parent]]
    (test/testing (name phase)
      (let [directory (temporary-directory)]
        (try
          (let [store (clio/open-store directory)
                port (clio/make-observations-adapter store)
                record (location)
                refused (with-force-refusal
                          phase #(attempt (fn [] ((:record-repository-location! port) record))))
                visible-bytes (fs/read-text (:file store))]
            (test/is (= :force-refused (get-in refused [:error :code])))
            (test/is (= 1 (count (clio/history store)))
                     "The failed append is visible, so validation alone cannot acknowledge durability")
            (dotimes [_ 2]
              (test/is (= :force-refused
                          (get-in (with-force-refusal phase #(attempt (fn [] (clio/open-store directory))))
                                  [:error :code]))))
            (let [force-file! fs/force-file!
                  forced (atom 0)
                  reopened (with-redefs [fs/force-file!
                                         (fn [channel]
                                           (swap! forced inc)
                                           (force-file! channel))]
                             (clio/open-store directory))]
              (test/is (pos? @forced) "A successful reopen forces the existing ledger inode")
              (test/is (= record ((:find-by-request-id (clio/make-observations-adapter reopened))
                                  (:observation/request-id record))))
              (test/is (= visible-bytes (fs/read-text (:file store)))
                       "Reopening fences the same event; it must not append a retry copy")))
          (finally (fs/remove-tree! directory)))))))
