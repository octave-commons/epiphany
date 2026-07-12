(ns epiphany.law.registry
  "The single source of truth for Epiphany's domain contracts.

  Merges every law namespace's schema map into one registry and exposes
  validators keyed by schema name. The registry itself is plain EDN data;
  `validator` is its executable form."
  (:require [epiphany.law.git :as git]
            [epiphany.law.markdown :as markdown]
            [epiphany.law.observation :as observation]
            [epiphany.law.ports :as ports]
            [epiphany.law.selection :as selection]
            [malli.core :as m]
            [malli.registry :as mr]))

(def schemas
  "Registry data: schema name -> schema body, all EDN-serializable."
  (merge git/schemas observation/schemas selection/schemas
          {"git-port"                        ports/git-port-schema
           "repository-metadata-port"        ports/repository-metadata-port-schema
           "observations-port"               ports/observations-port-schema
           "index-port"                      ports/index-port-schema
           "embeddings-port"                 ports/embeddings-port-schema
           "application/ports"               ports/application-ports-schema}))

(def ^:private registry
  (mr/composite-registry m/default-registry schemas))

(defn schema
  "Resolve a named contract from the registry."
  [schema-name]
  (m/schema [:ref schema-name] {:registry registry}))

(def validator
  "schema name -> compiled predicate. Memoized: contracts are static data."
  (memoize (fn [schema-name] (m/validator (schema schema-name)))))

(defn valid?
  "Does `value` satisfy the named contract?"
  [schema-name value]
  ((validator schema-name) value))

(defn explain
  "Malli explanation for a failing value, nil when valid."
  [schema-name value]
  (m/explain (schema schema-name) value))
