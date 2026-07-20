(ns epiphany.static.interop-inventory
  "Java-interop inventory + ratchet (STYLE.md layer laws). Adapter interop
   (infra/shape) is measured, not banned; the ratchet only fires on new
   direct interop appearing in law/domain/application, which must stay
   free of Java/SDK imports and dot/static calls per STYLE.md's table.
   Pure counting logic lives here so it can be tested against synthetic
   inventories, not just the real tree."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [epiphany.static.boundary-check :as bc]))

(def ratcheted-quadrants #{:law :domain :application})

(defn- import-classes
  "Class names declared in a (ns ...) form's :import clause, e.g.
   [java.util Date] [java.util UUID] -> #{java.util.Date java.util.UUID}."
  [ns-form]
  (let [import-clause (some #(when (and (seq? %) (= (first %) :import)) %)
                            (drop 2 ns-form))]
    (into #{}
          (mapcat (fn [entry]
                    (if (sequential? entry)
                      (let [pkg (first entry)]
                        (map #(symbol (str pkg "." %)) (rest entry)))
                      [entry])))
          (rest import-clause))))

(defn count-source-interop
  "Regex counts over raw source text: instance dot-calls `(.method ...)`,
   static calls `(Class/method ...)`, and Java type hints `^Class`. Regex
   over our own trusted source tree, not external input — deterministic
   and adequate for an inventory ratchet, not a full AST analysis."
  [text]
  {:dot-calls (count (re-seq #"\(\.[a-zA-Z]" text))
   :static-calls (count (re-seq #"\([A-Z][A-Za-z0-9_.]*\/[a-zA-Z]" text))
   :type-hints (count (re-seq #"(?<!:)\^[A-Z][A-Za-z0-9_.$]*" text))})

(defn file->inventory
  [file]
  (let [text (slurp file)
        ns-form (bc/read-ns-form file)
        [ns-sym _requires] (bc/ns-form->requires ns-form)
        counts (count-source-interop text)]
    {:ns ns-sym
     :quadrant (bc/quadrant-of ns-sym)
     :imports (import-classes ns-form)
     :dot-calls (:dot-calls counts)
     :static-calls (:static-calls counts)
     :type-hints (:type-hints counts)}))

(defn interop-count
  "Total direct-interop surface for one namespace's inventory entry."
  [{:keys [imports dot-calls static-calls type-hints]}]
  (+ (count imports) dot-calls static-calls type-hints))

(defn scan-source-tree
  [src-root]
  (into {}
        (comp (filter #(.isFile %))
              (filter #(.endsWith (.getName %) ".clj"))
              (keep (fn [file]
                      (try
                        (let [inv (file->inventory file)]
                          (when (:ns inv) [(:ns inv) inv]))
                        (catch Exception _ nil)))))
        (file-seq (io/file src-root))))

(defn ratchet-violations
  "baseline/current: ns-sym -> inventory map (as scan-source-tree returns).
   exceptions: set of ns-syms with a recorded, dated grandfather exception.
   Only law/domain/application namespaces are ratcheted — infra/shape
   interop is expected and unbounded here."
  [baseline current exceptions]
  (for [[ns-sym inv] current
        :when (contains? ratcheted-quadrants (:quadrant inv))
        :when (not (contains? exceptions ns-sym))
        :let [baseline-count (interop-count (get baseline ns-sym {:imports #{} :dot-calls 0 :static-calls 0 :type-hints 0}))
              current-count (interop-count inv)]
        :when (> current-count baseline-count)]
    {:ns ns-sym :baseline baseline-count :current current-count}))

(defn- summary-for-baseline
  "Strip the map down to plain EDN-safe data (sets of symbols, ints) for
   the committed baseline file."
  [inventory]
  (into (sorted-map)
        (map (fn [[ns-sym inv]]
               [ns-sym (-> inv
                           (dissoc :ns)
                           (update :imports (comp set sort)))]))
        inventory))

(def baseline-path "reports/interop.edn")

(defn -main [& args]
  (let [current (scan-source-tree "src")]
    (if (= (first args) "--write")
      (do
        (io/make-parents baseline-path)
        (spit baseline-path (with-out-str
                               (println ";; Reviewed interop baseline (ENG-017H). Regenerate with")
                               (println ";; `clojure -M:boundary-check --write` only after reviewing the diff.")
                               (pprint/pprint (summary-for-baseline current))))
        (println (str "Wrote " baseline-path)))
      (let [baseline-raw (edn/read-string (slurp baseline-path))
            baseline (into {} (map (fn [[k v]] [k (assoc v :ns k)])) baseline-raw)
            violations (ratchet-violations baseline current #{})]
        (if (seq violations)
          (do
            (doseq [{:keys [ns baseline current]} violations]
              (println (format "INTEROP RATCHET VIOLATION: %s grew from %d to %d direct interop points"
                                ns baseline current)))
            (System/exit 1))
          (do
            (println "Interop ratchet: clean (law/domain/application unchanged or reduced).")
            (System/exit 0)))))))
