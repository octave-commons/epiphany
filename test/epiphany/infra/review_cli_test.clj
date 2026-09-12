(ns epiphany.infra.review-cli-test
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [clojure.tools.cli :as cli]
            [epiphany.application.commands :as commands]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.main :as main]
            [epiphany.infra.profile :as profile])
  (:import [org.eclipse.jgit.api Git]))

(deftest all-profile-options-accept-the-documented-colon-spelling
  (doseq [options [main/register-options main/status-options main/search-options
                   main/ingest-options main/serve-options main/diff-options
                   main/inbox-options main/inbox-decide-options main/export-options]
          profile [:local :edn :services]
          spelling [(name profile) (str profile)]]
    (let [parsed (cli/parse-opts ["--profile" spelling] options)]
      (is (nil? (:errors parsed)) (pr-str {:profile spelling :errors (:errors parsed)}))
      (is (= profile (get-in parsed [:options :profile]))))))

(deftest generated-register-command-id-is-visible-durable-and-retryable
  (let [directory (str (System/getProperty "java.io.tmpdir")
                       "/epiphany-register-review-" (random-uuid))
        repository (str directory "/repo")
        ledger (str directory "/observations")
        resolve-adapters profile/resolve-adapters
        decode commands/decode
        decoded (atom [])]
    (try
      (fs/ensure-dir! repository)
      (with-open [_git (.call (.setDirectory (Git/init) (java.io.File. repository)))])
      (with-redefs [profile/resolve-adapters
                    (fn [options]
                      (resolve-adapters (assoc options :edn-dir ledger
                                               :index-dir (str directory "/index"))))
                    commands/decode
                    (fn [candidate] (swap! decoded conj candidate) (decode candidate))]
        (let [first-result (main/run ["register" "--profile" "edn" repository])
              request-id (:request-id (first @decoded))
              before (clio/history (clio/open-store ledger))
              retry (when request-id
                      (main/run ["register" "--profile" ":edn"
                                 "--request-id" (str request-id) repository]))]
          (is (zero? (:exit first-result)))
          (is (uuid? request-id))
          (is (and request-id (string/includes? (:out first-result) (str request-id))))
          (is (= request-id (get-in (first before) [:event/data :arguments 0 :observation/request-id])))
          (is (= first-result retry))
          (is (= before (clio/history (clio/open-store ledger))))))
      (finally (fs/remove-tree! directory)))))
