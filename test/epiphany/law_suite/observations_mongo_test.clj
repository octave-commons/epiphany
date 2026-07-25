(ns epiphany.law-suite.observations-mongo-test
  "ENG-017E: drive the shared observation-port law suite (ENG-017D)
   against the real MongoDB adapter, and prove differential parity with
   the ENG-017C in-memory reference adapter — both adapters must report
   identical outcome categories per law.

   Requires a running MongoDB instance (localhost:27017).

   Isolation: every law gets a fresh, uniquely-prefixed set of
   collections (the in-memory reference gets a fresh atom per law; this
   is the Mongo equivalent). Without per-law isolation the suite's
   shared fixture :observation/id values — which become Mongo _ids —
   contaminate later laws through the _id uniqueness constraint, and
   idempotency laws judge stale documents instead of their own writes.
   All law connections are dropped and disconnected in teardown; only
   Epiphany-owned prefixed collections are touched. Tagged ^:integration
   so it only runs with the :integration profile."
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.adapters.mongo :as mongo]
            [epiphany.law-suite.observations-laws :as laws]))

(def ^:private test-uri
  "MongoDB URI with authentication for integration tests."
  "mongodb://openplanner:GamG7Ly2g7eyMJoIa-4zS17eAUlWiUup@127.0.0.1:27017/openplanner?authSource=openplanner")

(def ^:private law-conns (atom []))

(defn- fresh-law-port
  "Open a uniquely-prefixed connection (fresh, empty collections) and
   return a Mongo observations port over it. Tracked for teardown."
  []
  (let [conn (mongo/connect! {:uri               test-uri
                              :database          "openplanner"
                              :collection-prefix (str "epiphany_law_"
                                                      (java.util.UUID/randomUUID)
                                                      "_")})]
    (swap! law-conns conj conn)
    (mongo/make-observations-adapter conn)))

(use-fixtures :each
  (fn [f]
    (try
      (f)
      (finally
        (doseq [conn @law-conns]
          (mongo/clean-test-db! conn)
          (mongo/disconnect! conn))
        (reset! law-conns [])))))

(def ^:private all-capabilities
  #{:schema-validation :idempotency :export-import})

(defn- run-mongo-laws
  []
  (laws/observations-laws
   {:make-port    fresh-law-port
    :capabilities all-capabilities}))

(defn- run-reference-laws
  []
  (laws/observations-laws
   {:make-port    (fn [] (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))
    :capabilities all-capabilities}))

(deftest ^:integration mongo-adapter-passes-all-laws
  (testing "the Mongo adapter passes the identical ENG-017D law suite the reference adapter passes, for every write op"
    (let [outcomes (run-mongo-laws)]
      (is (empty? (laws/failed-laws outcomes))
          (str "no law may fail for the Mongo adapter; failures: "
               (pr-str (select-keys outcomes (laws/failed-laws outcomes)))))
      (is (empty? (laws/skipped-laws outcomes))
          "with every capability declared, no law may be skipped")
      (testing "universal laws hold for every registered write op"
        (doseq [op (keys laws/op-fixtures)
                law [:valid-write-accepted
                     :invalid-write-rejected
                     :rejection-leaves-state-unchanged]]
          (is (= :pass (:outcome (get outcomes [op law])))
              (str "law " [op law] " must pass, got " (pr-str (get outcomes [op law]))))))
      (testing "idempotency laws hold for request-id-bearing record kinds"
        (doseq [op [:record-repository-location!
                    :record-review-decision!
                    :record-lineage-candidate!]
                law [:idempotent-replay-stable :changed-content-replay]]
          (is (= :pass (:outcome (get outcomes [op law])))
              (str "law " [op law] " must pass, got " (pr-str (get outcomes [op law])))))))))

(deftest ^:integration mongo-and-reference-adapters-report-identical-outcomes
  (testing "differential requirement: in-memory and Mongo agree on every shared law's outcome category"
    (let [mongo-outcomes     (run-mongo-laws)
          reference-outcomes (run-reference-laws)]
      (doseq [law (into #{} (concat (keys mongo-outcomes) (keys reference-outcomes)))]
        (is (= (:outcome (get reference-outcomes law))
               (:outcome (get mongo-outcomes law)))
            (str "law " law " outcome category must agree across adapters; reference: "
                 (pr-str (get reference-outcomes law)) " mongo: "
                 (pr-str (get mongo-outcomes law))))))))
