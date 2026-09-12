(ns epiphany.domain.observation-invocation
  "Normalize validated port arguments into command metadata and replay arguments."
  (:require [epiphany.law.clio-observations :as law]))

(defn write-invocation
  "Preserve ordinary argument vectors; separate an identified clear's UUID."
  [operation arguments]
  (law/assert-write-arguments! operation arguments)
  (if (= operation :clear-all!)
    {:arguments [] :command-id (first arguments)}
    {:arguments arguments}))
