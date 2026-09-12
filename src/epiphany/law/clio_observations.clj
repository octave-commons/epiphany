(ns epiphany.law.clio-observations
  "Finite accepted-operation contract for durable observation-port replay."
  (:require [clio.law.schema :as schema]
            [epiphany.law.operations :as operations]))

(def write-operations
  "Only these observation operations may change the durable projection."
  (conj (operations/registered-operations) :clear-all!))

(def catalog
  "Content-addressed Clio schema; input records retain their existing contracts."
  {:epiphany.observations/operation-accepted
   (schema/event-schema
    :epiphany.observations/operation-accepted
    [:map {:closed true}
     [:operation (into [:enum] (sort write-operations))]
     [:arguments [:vector :any]]
     [:command-id {:optional true} :uuid]
     [:before-hash schema/hash-schema]
     [:after-hash schema/hash-schema]
     [:result :nil]])})

(defn assert-arguments!
  "Reject unknown operations or wrong arity before replay or persistence."
  [operation arguments]
  (when-not (and (contains? write-operations operation)
                 (vector? arguments)
                 (= (if (= :clear-all! operation) 0 1) (count arguments)))
    (throw (ex-info "Invalid durable observation invocation"
                    {:code :invalid-observation-invocation :operation operation})))
  arguments)

(defn assert-write-arguments!
  "Require a caller UUID for new clears while retaining zero-argument historical replay."
  [operation arguments]
  (if (= operation :clear-all!)
    (when-not (and (vector? arguments) (= 1 (count arguments)) (uuid? (first arguments)))
      (throw (ex-info "Durable clear requires one caller-provided command UUID"
                      {:code :invalid-observation-invocation :operation operation})))
    (assert-arguments! operation arguments))
  arguments)

(defn assert-command!
  "A replayed command ID is lawful only for a clear with its UUID identity."
  [operation command-id]
  (when (and (some? command-id) (not (and (= operation :clear-all!) (uuid? command-id))))
    (throw (ex-info "Invalid durable observation command identity"
                    {:code :integrity/invalid-command :operation operation})))
  command-id)
