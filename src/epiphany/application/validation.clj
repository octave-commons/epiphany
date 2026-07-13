(ns epiphany.application.validation
  "Validation gateway for observations-port writes.

  Wraps every public durable write at the composition root so no
  application flow can reach an adapter with an unvalidated record.
  Consumes the ENG-017A operation registry for schema resolution.

  Read operations (:find-by-request-id, :list-*, :export-all) pass
  through unwrapped. Write operations (:record-*, :import-all) are
  validated before delegation."
  (:require [epiphany.law.operations :as operations]
            [epiphany.law.registry :as registry]))

(defn- strip-explanation
  "Strip validated values from a Malli explanation, keeping only
  error paths and messages. Never embeds the record's content."
  [explain]
  (when explain
    (mapv (fn [error]
            (select-keys error [:path :schema :message]))
          (:errors explain))))

(defn- validate-or-throw!
  "Validate `record` against the schema registered for `op`.
  Throws :schema-validation-failed with stable error data on failure."
  [op record]
  (let [entry (operations/schema-for-operation op)]
    (when (:code entry)
      (throw (ex-info (str "Unregistered write operation: " op)
                      entry)))
    (when-let [schema-name (:input-schema entry)]
      (when-let [explain (registry/explain schema-name record)]
        (throw (ex-info (str "Schema validation failed for " op)
                        {:code :schema-validation-failed
                         :operation op
                         :schema/name schema-name
                         :explanation (strip-explanation explain)}))))))

(defn wrap-write
  "Wrap a single write function with validation.
  Returns a function that validates before delegating."
  [op f]
  (fn [& args]
    ;; The write functions receive one argument: the record/map.
    (let [record (first args)]
      (validate-or-throw! op record)
      (apply f args))))

(defn validating-observations-port
  "Wrap an observations port so every write operation validates its
  input against the ENG-017A registry before delegating.

  Read operations pass through unchanged."
  [port]
  (reduce-kv
   (fn [m op f]
     (if (contains? operations/port-write-operations op)
       (assoc m op (wrap-write op f))
       (assoc m op f)))
   {}
   port))
