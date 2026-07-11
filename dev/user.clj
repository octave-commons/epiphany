(ns user
  "Dev-time helpers, on the classpath only under the :dev alias.")

(defn kaocha
  "Run kaocha test suites from the REPL, e.g. (kaocha) or (kaocha :unit)."
  [& suite-ids]
  (if (seq suite-ids)
    (apply (requiring-resolve 'kaocha.repl/run) suite-ids)
    ((requiring-resolve 'kaocha.repl/run-all))))
