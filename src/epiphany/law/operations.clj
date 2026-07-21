(ns epiphany.law.operations
  "The single source of truth for which schema governs each durable write.

  Maps every public observations-port write operation to its named,
  versioned Malli schema. This is the contract that enforcement
  wrappers (ENG-017B) and adapter tests (ENG-017C/D) read; it changes
  no adapter behavior itself.

  Registry entries are plain EDN data — usable by tests, docs
  generation, and the future checker without invoking any function."
  (:require [clojure.string :as str]
            [epiphany.law.ports :as ports]))

;; ---------------------------------------------------------------------------
;; Operation registry
;;
;; One entry per public observations-port write. Read operations
;; (:find-by-request-id, :list-*, :export-all) are not registered
;; because they do not persist records.

(def operations
  "Map of operation keyword -> registry entry. Each entry:
    :input-schema  — named schema the record must satisfy
    :version       — required :observation/schema-version value
    :persistence   — :append-only (records are never updated or deleted)"
  {:record-repository-location!
   {:input-schema "observation/repository-location-v1"
    :version 1
    :persistence :append-only}

   :record-revision-at-path!
   {:input-schema "observation/revision-at-path-v1"
    :version 1
    :persistence :append-only}

   :record-ingestion-run!
   {:input-schema "observation/ingestion-run-v1"
    :version 1
    :persistence :append-only}

   :record-checkpoint!
   {:input-schema "observation/projection-checkpoint-v1"
    :version 1
    :persistence :append-only}

   :record-section-extraction!
   {:input-schema "observation/section-extraction-v1"
    :version 1
    :persistence :append-only}

   :record-review-decision!
   {:input-schema "observation/review-decision-v1"
    :version 1
    :persistence :append-only}

   :record-lineage-candidate!
   {:input-schema "observation/lineage-candidate-v1"
    :version 1
    :persistence :append-only}

   ;; Bulk import — payload references the same per-collection schemas;
   ;; enforcement is ENG-017F scope but the mapping lives here.
   :import-all
   {:input-schema nil ;; bulk payload; enforcement deferred
    :version nil
    :persistence :append-only}})

;; ---------------------------------------------------------------------------
;; Lookup

(defn schema-for-operation
  "Return the registry entry for `op`, or a stable error datum."
  [op]
  (if-let [entry (get operations op)]
    entry
    {:code :unregistered-write-operation
     :operation op}))

(defn registered-operations
  "Return the set of registered write-operation keywords."
  []
  (set (keys operations)))

(defn registered-write-operations
  "Return only the write operations (excludes :import-all bulk)."
  []
  (disj (registered-operations) :import-all))

;; ---------------------------------------------------------------------------
;; Version check

(defn validate-version
  "Check that a record's claimed schema-version matches the registry.
  Returns nil when valid, stable error datum when mismatched."
  [op record]
  (let [entry (get operations op)]
    (cond
      (nil? entry)
      {:code :unregistered-write-operation :operation op}

      (nil? (:version entry))
      nil ;; bulk operations have no per-record version check

      (not= (:version entry) (:observation/schema-version record))
      {:code :schema-version-mismatch
       :operation op
       :schema/name (:input-schema entry)
       :expected-version (:version entry)
       :actual-version (:observation/schema-version record)}

      :else nil)))

;; ---------------------------------------------------------------------------
;; Port operation classification + completeness set
;;
;; The write operations on the observations port: every :record-*! fn
;; plus :import-all. Read operations (:find-by-request-id, :list-*,
;; :export-all) are excluded because they do not persist records.

(defn write-operation?
  "True when `op` names a durable write on the observations port: any
  :record-*! operation plus :import-all. Read operations (:find-*,
  :list-*, :export-all) return false. Pure classification over the
  operation keyword's name — no I/O."
  [op]
  (or (= op :import-all)
      (and (keyword? op)
           (let [n (name op)]
             (and (str/starts-with? n "record-")
                  (str/ends-with? n "!"))))))

(defn- map-schema-entry-keys
  "Extract the entry keys from a raw Malli `:map` schema vector,
  skipping the leading `:map` marker and the optional properties map."
  [schema]
  (->> schema
       (drop 1)
       (remove map?)
       (map first)))

(def port-write-operations
  "Set of write-operation keywords derived from the observations-port
  schema in `law/ports`. Computed by classifying every declared port
  entry with `write-operation?`, so adding a new write to the port
  schema without a matching registry entry turns the completeness
  suite red by construction (never a hand-maintained parallel copy)."
  (into #{}
        (filter write-operation?)
        (map-schema-entry-keys ports/observations-port-schema)))
