(ns epiphany.extern.resource-selection
  "Bounded, non-evaluating selection-policy file boundary."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m]
            [epiphany.law.resource-selection :as law]))

(defn- read-value! [options reader]
  (try
    (edn/read options reader)
    (catch StackOverflowError _
      (throw (ex-info "Selection policy exceeds reader nesting limit" {:code :bad-request})))))

(defn read-policy!
  "Read exactly one closed-schema EDN value and bind it to the explicit repo argument."
  [file repository-path]
  (when file
    (when (> (.length (io/file file)) 1048576)
      (throw (ex-info "Selection policy exceeds 1 MiB" {:code :bad-request})))
    (with-open [reader (java.io.PushbackReader. (io/reader file :encoding "UTF-8"))]
      (let [options {:eof ::eof :readers {} :default (fn [_ _] (throw (ex-info "Tagged selection policy is forbidden" {:code :bad-request})))}
            policy (read-value! options reader)]
        (when-not (and (m/validate law/policy-schema policy)
                       (every? law/git-path? (:selection/paths policy))
                       (= ::eof (read-value! options reader))
                       (= repository-path (:selection/repository policy)))
          (throw (ex-info "Invalid selection policy or repository scope" {:code :bad-request})))
        (update policy :selection/paths #(vec (sort (distinct %))))))))
