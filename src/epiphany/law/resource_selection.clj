(ns epiphany.law.resource-selection
  "Exact-path selection is command data, never executable reader configuration."
  (:require [clojure.string :as str]))

(defn git-path?
  "Admit literal repository-relative Git paths without filesystem traversal."
  [value]
  (and (string? value) (seq value)
       (not (str/starts-with? value "/"))
       (not (str/includes? value "\u0000"))
       (not-any? #{"" "." ".."} (str/split value #"/" -1))))

(def policy-schema
  "Versioned, repository-scoped literal paths. Wildcard characters are literal."
  [:map {:closed true}
   [:selection/version [:= 1]]
   [:selection/repository [:string {:min 1}]]
   [:selection/paths
    [:vector {:min 1 :max 4096}
     [:and [:string {:min 1 :max 8192}]
      [:re "(?s)^(?!/)(?!.*\\u0000)(?!.*(?:^|/)\\.{1,2}(?:/|$))(?!.*//)(?!.*/$).+$"]]]]])
