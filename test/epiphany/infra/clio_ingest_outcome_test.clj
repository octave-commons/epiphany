(ns epiphany.infra.clio-ingest-outcome-test
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [epiphany.domain.extraction-projection :as extraction]
            [epiphany.domain.observation-admission :as admission]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.git :as git]
            [epiphany.infra.main :as main]
            [epiphany.law.registry :as registry]))

(defn- git! [directory & arguments]
  (let [result (apply shell/sh "git" "-C" directory arguments)]
    (when-not (zero? (:exit result))
      (throw (ex-info "Literal Git fixture command failed" result)))
    result))

(defn- with-repository [f]
  (let [directory (str (System/getProperty "java.io.tmpdir") "/epiphany-ingest-outcome-" (random-uuid))
        repository (str directory "/repo")]
    (try
      (.mkdirs (io/file repository))
      (git! repository "init" "-q")
      (git! repository "config" "user.name" "Epiphany test")
      (git! repository "config" "user.email" "fixture@example.invalid")
      (spit (io/file repository "notes.md") "# Accepted facts\n\nConcurrency preserves one observation.\n")
      (git! repository "add" "notes.md")
      (git! repository "commit" "-qm" "Literal extraction fixture")
      (let [store (clio/open-store (str directory "/ledger"))
            ports (mapv (fn [_] (clio/make-observations-adapter
                                 (clio/open-store (str directory "/ledger")))) (range 2))]
        (f {:repository repository :store store :ports ports :resource-id (random-uuid)}))
      (finally (fs/remove-tree! directory)))))

(defn- barrier-read [port operation barrier]
  (update port operation
          (fn [read-fn]
            (fn [& arguments]
              (let [snapshot (apply read-fn arguments)]
                (.await barrier 20 java.util.concurrent.TimeUnit/SECONDS)
                snapshot)))))

(defn- concurrent-results [f inputs]
  (let [workers (mapv #(future (f %)) inputs)]
    (try
      (mapv #(deref % 30000 ::timeout) workers)
      (finally
        (doseq [worker workers]
          (when-not (realized? worker) (future-cancel worker)))))))

(defn- extraction-ports [repository observations index-fn]
  {:observations observations
   :git {:read-blob (fn [_ oid] (git/read-blob repository oid))}
   :index {:index-sections! index-fn}})

(deftest write-outcomes-retain-legacy-nil-and-reject-ambiguous-results
  (doseq [[result stored?] [[nil true]
                            [{:observation/write-status :accepted} true]
                            [{:observation/write-status :duplicate} false]]]
    (is (registry/valid? "observation/write-result" result))
    (is (= stored? (admission/newly-stored? result))))
  (doseq [result [false {} {:observation/write-status :unknown}
                  {:observation/write-status :accepted :unexpected true}]]
    (is (not (registry/valid? "observation/write-result" result)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid observation write result"
                          (admission/newly-stored? result)))))

(deftest overlapping-revision-projections-count-only-accepted-writes
  (with-repository
    (fn [{:keys [repository store ports resource-id]}]
      (let [barrier (java.util.concurrent.CyclicBarrier. 2)
            readers (mapv #(barrier-read % :list-revision-at-path-by-resource barrier) ports)
            results (concurrent-results
                     #(#'main/project-revision-at-path! % repository resource-id #{"HEAD"}) readers)]
        (is (= [0 1] (sort results)) "The losing projection must report zero newly stored revisions")
        (is (= 1 (count ((:list-revision-at-path-by-resource (first ports)) resource-id))))
        (is (= 1 (count (clio/history store))))
        (is (every? nil? (map #(get-in % [:event/data :result]) (clio/history store)))
            "Historical reference results remain nil")))))

(deftest overlapping-extraction-projections-count-and-index-only-accepted-records
  (with-repository
    (fn [{:keys [repository ports resource-id]}]
      (#'main/project-revision-at-path! (first ports) repository resource-id #{"HEAD"})
      (let [barrier (java.util.concurrent.CyclicBarrier. 2)
            indexed (atom [])
            readers (mapv #(barrier-read % :list-section-extractions-by-revision barrier) ports)
            results (concurrent-results
                     (fn [observations]
                       (extraction/run-extraction-projection
                        (extraction-ports repository observations #(swap! indexed conj %))
                        {:resource-id resource-id :ingestion-run-id (random-uuid)})) readers)
            revision (first ((:list-revision-at-path-by-resource (first ports)) resource-id))
            stored ((:list-section-extractions-by-revision (first ports)) (:revision-at-path/id revision))]
        (is (= [0 1] (sort (map :projection/sections-extracted results))))
        (is (every? empty? (map :projection/failures results)))
        (is (= 1 (count stored)))
        (is (= 1 (count @indexed)) "A discarded observation must never reach the index")
        (is (= (mapv :observation/id stored) (mapv :observation/id @indexed)))))))

(deftest index-failure-does-not-retract-a-durable-extraction
  (with-repository
    (fn [{:keys [repository ports resource-id]}]
      (let [observations (first ports)]
        (#'main/project-revision-at-path! observations repository resource-id #{"HEAD"})
        (let [result (extraction/run-extraction-projection
                      (extraction-ports repository observations
                                        (fn [_] (throw (ex-info "Index unavailable" {:code :index-unavailable}))))
                      {:resource-id resource-id :ingestion-run-id (random-uuid)})
              revision (first ((:list-revision-at-path-by-resource observations) resource-id))]
          (is (= 1 (:projection/sections-extracted result)) "The canonical append succeeded before the index failed")
          (is (= 1 (count ((:list-section-extractions-by-revision observations) (:revision-at-path/id revision)))))
          (is (= 1 (count (:projection/failures result))))
          (is (= "Index unavailable" (:failure/message (first (:projection/failures result))))))))))
