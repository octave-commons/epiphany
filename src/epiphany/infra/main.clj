(ns epiphany.infra.main
  "Single executable entry point for `epiphany` (short alias: `ep`).
  At this stage (US-000A) it only reports version and help; commands
  arrive with later cards."
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli])
  (:gen-class))

(def version "0.1.0")

(def cli-options
  [["-h" "--help" "Show this help and exit."]
   ["-v" "--version" "Show the version and exit."]])

(defn usage [options-summary]
  (string/join
   \newline
   ["epiphany — local-first, Git-backed knowledge archaeology."
    ""
    "Usage: epiphany [options]"
    "       ep [options]"
    ""
    "`epiphany` is the canonical executable; `ep` invokes the same entry point."
    ""
    "Options:"
    options-summary
    ""
    "Commands arrive with later cards; this build only reports version/help."]))

(defn run
  "Interpret command-line arguments without side effects.
  Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      errors {:exit 1
              :out (string/join \newline (concat errors ["" (usage summary)]))}
      (:version options) {:exit 0 :out (str "epiphany " version)}
      :else {:exit 0 :out (usage summary)})))

(defn -main [& args]
  (let [{:keys [exit out]} (run args)]
    (println out)
    (System/exit exit)))
