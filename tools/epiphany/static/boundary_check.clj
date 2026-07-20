(ns epiphany.static.boundary-check
  "Layer-boundary gate for the law/shape/domain/application/infra quadrants
   declared in STYLE.md. Pure analysis over a map of ns-symbol -> required
   ns-symbols; the filesystem scan is a thin, separately-testable shell
   around it so the rule itself has no I/O to fake in tests."
  (:require [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(def quadrant-order [:law :shape :domain :application :infra])

(def allowed-quadrants
  "Which quadrants a namespace in this quadrant may require, among
   epiphany.* namespaces. infra may require anything lower; law may
   require only itself."
  {:law #{:law}
   :shape #{:law :shape}
   :domain #{:law :shape :domain}
   :application #{:law :shape :domain :application}
   :infra (set quadrant-order)})

(defn quadrant-of
  "epiphany.domain.foo -> :domain; epiphany.integration.bar or a non-epiphany
   ns -> nil (out of scope for this gate)."
  [ns-sym]
  (let [segments (clojure.string/split (name ns-sym) #"\.")]
    (when (and (= (first segments) "epiphany") (>= (count segments) 2))
      (some #{(keyword (second segments))} quadrant-order))))

(defn- require-form->ns-syms
  "A single :require entry is a symbol, or a vector [ns & opts]. libspecs
   inside a prefix list are not used anywhere in this codebase; unsupported."
  [entry]
  (cond
    (symbol? entry) [entry]
    (vector? entry) [(first entry)]
    :else []))

(defn ns-form->requires
  "Given a parsed (ns ...) form, return [ns-sym #{required-ns-syms}]."
  [ns-form]
  (let [ns-sym (second ns-form)
        require-clause (some #(when (and (seq? %) (= (first %) :require)) %)
                             (drop 2 ns-form))
        requires (into #{} (mapcat require-form->ns-syms) (rest require-clause))]
    [ns-sym requires]))

(defn read-ns-form
  "Read only the leading (ns ...) form from a source file, tolerating
   arbitrary code after it (reader macros, deftype, etc. below the ns form
   are never reached)."
  [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (read {:eof nil} rdr))))

(defn find-violations
  "graph: map of ns-sym -> set of required ns-syms (epiphany.* only).
   Returns a seq of {:ns :requires :quadrant :offending-quadrant} for every
   required namespace whose quadrant this namespace's quadrant may not
   depend on."
  [graph]
  (for [[ns-sym requires] graph
        :let [from-quadrant (quadrant-of ns-sym)]
        :when from-quadrant
        required requires
        :let [to-quadrant (quadrant-of required)]
        :when (and to-quadrant (not (contains? (allowed-quadrants from-quadrant) to-quadrant)))]
    {:ns ns-sym
     :requires required
     :quadrant from-quadrant
     :offending-quadrant to-quadrant}))

(defn scan-source-tree
  "Build the ns-sym -> requires graph by reading every .clj file's leading
   ns form under src-root."
  [src-root]
  (into {}
        (comp (filter #(.isFile %))
              (filter #(.endsWith (.getName %) ".clj"))
              (keep (fn [file]
                      (try
                        (ns-form->requires (read-ns-form file))
                        (catch Exception _ nil)))))
        (file-seq (io/file src-root))))

(defn -main [& _args]
  (let [violations (find-violations (scan-source-tree "src"))]
    (if (seq violations)
      (do
        (doseq [{:keys [ns requires quadrant offending-quadrant]} violations]
          (println (format "BOUNDARY VIOLATION: %s (%s) requires %s (%s)"
                            ns (name quadrant) requires (name offending-quadrant))))
        (println (format "\n%d layer-boundary violation(s)." (count violations)))
        (System/exit 1))
      (do
        (println "Layer-boundary check: clean.")
        (System/exit 0)))))
