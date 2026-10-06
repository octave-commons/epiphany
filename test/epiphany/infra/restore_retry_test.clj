(ns epiphany.infra.restore-retry-test
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.test :refer [deftest is]]
            [epiphany.extern.backup-files :as files]
            [epiphany.infra.backup :as backup]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.law-suite.observations-laws :as laws]))

(deftest interrupted-restore-reuses-the-original-snapshot-after-reopen
  (let [directory (str (System/getProperty "java.io.tmpdir")
                       "/epiphany-restore-retry-" (random-uuid))
        ledger (str directory "/observations")
        backup-directory (str directory "/drill")
        backup-file (str backup-directory "/backup.edn")
        command-id (random-uuid)
        record ((get-in laws/op-fixtures [:record-repository-location! :make-valid])
                (random-uuid))
        git {:common-git-directory identity}]
    (try
      (let [port (clio/make-observations-adapter (clio/open-store ledger))]
        ((:record-repository-location! port) record)
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo #"Interrupted import"
             (backup/restore-drill
              (assoc port :import-all
                     (fn [_] (throw (ex-info "Interrupted import" {:fault :import}))))
              git backup-directory command-id)))
        (is (empty? (get ((:export-all port)) "repository-location")))
        (let [original-bytes (fs/read-text backup-file)
              reopened (clio/make-observations-adapter (clio/open-store ledger))
              report (backup/restore-drill reopened git backup-directory command-id)]
          (is (= :complete (:drill-status report)))
          (is (= original-bytes (fs/read-text backup-file)))
          (is (= record ((:find-by-request-id reopened) (:observation/request-id record))))
          (is (= 1 (:total-docs (:export report))))
          (is (= 1 (count (filter #(= command-id (get-in % [:event/data :command-id]))
                                  (clio/history (clio/open-store ledger))))))))
      (finally (fs/remove-tree! directory)))))

(deftest failed-snapshot-fence-prevents-clear-and-retry-keeps-published-bytes
  (let [directory (str (System/getProperty "java.io.tmpdir") "/epiphany-restore-fence-" (random-uuid))
        ledger (str directory "/observations")
        drill (str directory "/drill")
        command-id (random-uuid)
        record ((get-in laws/op-fixtures [:record-repository-location! :make-valid]) (random-uuid))]
    (try
      (let [store (clio/open-store ledger)
            port (clio/make-observations-adapter store)]
        ((:record-repository-location! port) record)
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo #"Injected backup force failure"
             (with-redefs [files/ensure-durable!
                           (fn [& _] (throw (ex-info "Injected backup force failure" {:fault :force})))]
               (backup/restore-drill port {:common-git-directory identity} drill command-id))))
        (is (= 1 (count (clio/history store))))
        (is (= record ((:find-by-request-id port) (:observation/request-id record))))
        (let [bytes (fs/read-text (str drill "/backup.edn"))
              reopened (clio/make-observations-adapter (clio/open-store ledger))]
          (is (= :complete (:drill-status
                            (backup/restore-drill reopened {:common-git-directory identity}
                                                  drill command-id))))
          (is (= bytes (fs/read-text (str drill "/backup.edn"))))))
      (finally (fs/remove-tree! directory)))))

(deftest conflicting-or-corrupt-backups-never-clear-or-replace-data
  (let [directory (str (System/getProperty "java.io.tmpdir") "/epiphany-restore-conflict-" (random-uuid))
        ledger (str directory "/observations")
        drill (str directory "/drill")
        file (str drill "/backup.edn")
        command-id (random-uuid)
        git {:common-git-directory identity}]
    (try
      (let [store (clio/open-store ledger)
            port (clio/make-observations-adapter store)]
        (backup/restore-drill port git drill command-id)
        (doseq [operation [#(backup/restore-drill port git drill (random-uuid))
                           #(backup/restore-drill port git drill)]]
          (let [bytes (fs/read-text file)
                history (clio/history store)]
            (is (thrown-with-msg? clojure.lang.ExceptionInfo #"identified|different" (operation)))
            (is (= bytes (fs/read-text file)))
            (is (= history (clio/history store)))))
        (doseq [corrupt ["nil" "{" "{}"]]
          (fs/write-text! file corrupt)
          (let [history (clio/history store)]
            (is (thrown? clojure.lang.ExceptionInfo (backup/restore-drill port git drill command-id)))
            (is (= corrupt (fs/read-text file)))
            (is (= history (clio/history store))))))
      (finally (fs/remove-tree! directory)))))
