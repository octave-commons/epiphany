(ns epiphany.domain.history-replacement-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.history-replacement :as hr]))

(deftest make-replacement-record-has-required-fields
  (let [now (java.util.Date.)
        old (java.util.Date. (- (.getTime now) 1000))
        record (hr/make-replacement-record
                {:resource-id       #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :ref-name          "refs/heads/main"
                 :old-oid           "aaa111"
                 :new-oid           "bbb222"
                 :old-observed-at   old
                 :new-observed-at   now})]
    (is (= :git/history-replacement-observed (:observation/type record)))
    (is (uuid? (:observation/id record)))
    (is (inst? (:observation/observed-at record)))
    (is (= "0.1.0" (:observation/adapter-version record)))
    (is (= 1 (:observation/schema-version record)))
    (is (= #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee" (:resource-id record)))
    (is (= "refs/heads/main" (:replacement/ref-name record)))
    (is (= "aaa111" (:replacement/old-oid record)))
    (is (= "bbb222" (:replacement/new-oid record)))
    (is (= old (:replacement/old-observed-at record)))
    (is (= now (:replacement/new-observed-at record)))))

(deftest make-replacement-record-with-request-id
  (let [rid (random-uuid)
        record (hr/make-replacement-record
                {:resource-id     #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :ref-name        "refs/heads/main"
                 :old-oid         "aaa"
                 :new-oid         "bbb"
                 :old-observed-at (java.util.Date.)
                 :new-observed-at (java.util.Date.)
                 :request-id      rid})]
    (is (= rid (:observation/request-id record)))))

(deftest detect-replacements-finds-changed-refs
  (let [previous {"refs/heads/main" "aaa" "refs/heads/dev" "bbb"}
        current  {"refs/heads/main" "xxx" "refs/heads/dev" "bbb"}
        result   (hr/detect-replacements previous current {})]
    (is (= 1 (count result)))
    (is (= "refs/heads/main" (:ref-name (first result))))
    (is (= "aaa" (:old-oid (first result))))
    (is (= "xxx" (:new-oid (first result))))))

(deftest detect-replacements-ignores-unchanged-refs
  (let [previous {"refs/heads/main" "aaa"}
        current  {"refs/heads/main" "aaa"}
        result   (hr/detect-replacements previous current {})]
    (is (empty? result))))

(deftest detect-replacements-ignores-new-refs
  (let [previous {}
        current  {"refs/heads/main" "aaa"}
        result   (hr/detect-replacements previous current {})]
    (is (empty? result))))

(deftest detect-replacements-ignores-missing-refs
  (let [previous {"refs/heads/main" "aaa"}
        current  {}
        result   (hr/detect-replacements previous current {})]
    (is (empty? result))))

(deftest detect-missing-refs-finds-gone-refs
  (let [previous {"refs/heads/main" "aaa" "refs/heads/dev" "bbb"}
        current  {"refs/heads/main" "aaa"}
        result   (hr/detect-missing-refs previous current {})]
    (is (= 1 (count result)))
    (is (= "refs/heads/dev" (:ref-name (first result))))
    (is (= "bbb" (:old-oid (first result))))))

(deftest detect-missing-refs-ignores-present-refs
  (let [previous {"refs/heads/main" "aaa"}
        current  {"refs/heads/main" "aaa"}
        result   (hr/detect-missing-refs previous current {})]
    (is (empty? result))))
