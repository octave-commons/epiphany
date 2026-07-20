(ns epiphany.parity.cli-http-test
  "ENG-017G: given equivalent input, the CLI and HTTP surfaces must agree on
  outcome category (accepted vs rejected), even though today they still
  route through separate ad hoc parsing/adapter-construction code rather
  than a single shared decode/execute/encode seam. That full command-
  vocabulary refactor (decode-cli/decode-http/execute/encode-cli/encode-
  http per the design doc) is deferred — see the ENG-017G card comment;
  this namespace locks down the outcome-parity contract that already
  holds today so a future refactor can't silently regress it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [epiphany.infra.main :as main]
            [epiphany.infra.http :as http]
            [epiphany.infra.adapters.in-memory :as in-memory]))

(defn- shell-git-resolve
  "Same resolution strategy epiphany.infra.main's :local register/serve
  paths use: shell out to `git rev-parse --git-common-dir`."
  [path]
  (let [{:keys [exit out err]}
        (clojure.java.shell/sh "git" "-C" path "rev-parse"
                               "--path-format=absolute" "--git-common-dir")]
    (if (zero? exit)
      (str/trim out)
      (throw (ex-info (str "Not a Git repository: " path)
                       {:repository-path path :git-error (str/trim err)})))))

(defn- http-adapters []
  (in-memory/make {:common-git-dir-fn shell-git-resolve}))

(defn- http-register [path]
  (let [app (http/create-handler (http-adapters))]
    (app {:request-method :post
          :uri "/api/v1/register"
          :body-params (if path {:path path} {})
          :headers {"content-type" "application/json"}})))

(defn- http-search [{:keys [query mode limit]}]
  (let [app (http/create-handler (http-adapters))]
    (app {:request-method :post
          :uri "/api/v1/search"
          :body-params (cond-> {}
                         query (assoc :query query)
                         mode (assoc :mode mode)
                         limit (assoc :limit limit))
          :headers {"content-type" "application/json"}})))

(defn- cli-outcome
  "0 -> :accepted; nonzero -> :rejected. Matches the CLI's own exit-code
  contract (0/1), which is the CLI's outcome-category encoding."
  [{:keys [exit]}]
  (if (zero? exit) :accepted :rejected))

(defn- http-outcome
  "2xx -> :accepted; 4xx -> :rejected; anything else is neither (a genuine
  parity break, not a valid third category, so tests fail loudly on it)."
  [{:keys [status]}]
  (cond
    (<= 200 status 299) :accepted
    (<= 400 status 499) :rejected
    :else (keyword (str "unexpected-status-" status))))

;; ---------------------------------------------------------------------------
;; register parity

(deftest register-missing-path-parity
  (testing "no path: both surfaces reject"
    (is (= :rejected (cli-outcome (main/run ["register"]))))
    (is (= :rejected (http-outcome (http-register nil))))))

(deftest register-non-git-path-parity
  (testing "a real but non-Git path: both surfaces reject"
    (is (= :rejected (cli-outcome (main/run ["register" "-p" :local "/tmp"]))))
    (is (= :rejected (http-outcome (http-register "/tmp"))))))

(deftest register-real-repo-path-parity
  (testing "this repo's own working tree: both surfaces accept"
    (is (= :accepted (cli-outcome (main/run ["register" "-p" :local "."]))))
    (is (= :accepted (http-outcome (http-register "."))))))

;; ---------------------------------------------------------------------------
;; search parity

(deftest search-missing-query-parity
  (testing "no query: both surfaces reject"
    (is (= :rejected (cli-outcome (main/run ["search"]))))
    (is (= :rejected (http-outcome (http-search {:query ""}))))))

(deftest search-invalid-mode-parity
  (testing "an unrecognized mode: both surfaces reject"
    (is (= :rejected (cli-outcome (main/run ["search" "-m" "bogus" "test"]))))
    (is (= :rejected (http-outcome (http-search {:query "test" :mode :bogus}))))))

(deftest search-valid-empty-index-parity
  (testing "a well-formed query against an empty index: both surfaces accept"
    (is (= :accepted (cli-outcome (main/run ["search" "test"]))))
    (is (= :accepted (http-outcome (http-search {:query "test" :mode :hybrid}))))))

(deftest search-limit-out-of-bounds-parity
  (testing "limit beyond the shared upper bound: both surfaces reject"
    (is (= :rejected (cli-outcome (main/run ["search" "-l" (str (inc http/max-search-limit)) "test"]))))
    (is (= :rejected (http-outcome (http-search {:query "test" :mode :hybrid
                                                  :limit (inc http/max-search-limit)}))))))
