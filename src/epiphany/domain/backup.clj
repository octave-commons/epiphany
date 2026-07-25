(ns epiphany.domain.backup
  "Backup, restore, and index rebuild for Epiphany.

    Git is canonical for blobs/trees. MongoDB stores metadata/observations.
    Lucene/vector indices are rebuildable projections. Backup preserves
    MongoDB state; restore repopulates it; rebuild regenerates indices
    from Git + restored Mongo data.

   Integrity outcomes (ENG-017F) are distinct, non-collapsible ex-info
   categories:
     :integrity/corrupt              — malformed payload, bad hash,
                                       count/collection mismatch,
                                       record fails its schema
     :integrity/unsupported-version  — unknown manifest or record version
     :source/unavailable             — a named source cannot be read
     (empty store)                   — genuinely no records; never
                                       confused with the above"
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [epiphany.law.operations :as operations]
            [epiphany.law.registry :as registry]))

(def ^:private backup-manifest-version 1)

(defn- sha256-base64
  "Content hash of `s`, used to detect a corrupted or hand-edited backup
   file independently of the (order-sensitive) collection counts."
  [^String s]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes s "UTF-8"))]
    (.encodeToString (java.util.Base64/getEncoder) digest)))

;; ---------------------------------------------------------------------------
;; Manifest + payload validation (ENG-017F)
;;
;; Every check runs BEFORE any mutation. A backup that fails any check
;; leaves the target port byte-identical.

(defn validate-record
  "Validate one decoded/imported record against its collection's schema
   and version. Returns nil when valid; throws a named integrity
   category on violation. Shared by backup import and adapter decode."
  [collection-key record]
  (let [schema-name (get operations/collection-schemas collection-key)]
    (when-not schema-name
      (throw (ex-info (str "Unknown collection in backup payload: " (pr-str collection-key))
                      {:code :integrity/corrupt
                       :collection collection-key})))
    (when-not (= operations/expected-record-version
                 (:observation/schema-version record))
      (throw (ex-info (str "Unsupported record schema version in collection " collection-key)
                      {:code :integrity/unsupported-version
                       :collection collection-key
                       :record-id (:observation/id record)
                       :expected-version operations/expected-record-version
                       :actual-version (:observation/schema-version record)})))
    (when-let [explanation (registry/explain schema-name record)]
      (throw (ex-info (str "Record fails schema " schema-name)
                      {:code :integrity/corrupt
                       :collection collection-key
                       :schema/name schema-name
                       :record-id (:observation/id record)
                       :explanation (mapv #(select-keys % [:path :schema :message])
                                          (:errors explanation))})))
    nil))

(defn validate-backup-payload
  "Validate a decoded backup payload map {:manifest ... :data ...}
   completely, BEFORE any mutation. Returns the payload when valid;
   throws a named integrity category on the first violation:
     :integrity/corrupt             — malformed shape, unknown collection,
                                      collection/count mismatch, bad
                                      content-hash, record fails schema
     :integrity/unsupported-version — unknown manifest or record version"
  [{:keys [manifest data] :as payload}]
  (when-not (and (map? manifest) (map? data))
    (throw (ex-info "Malformed backup payload: expected {:manifest ... :data ...}"
                    {:code :integrity/corrupt
                     :payload-keys (keys payload)})))
  (when-not (= (:format manifest) :epiphany-backup-v1)
    (throw (ex-info "Unsupported backup format"
                    {:code :integrity/corrupt
                     :format (:format manifest)})))
  (when-not (= (:version manifest) backup-manifest-version)
    (throw (ex-info "Unsupported backup manifest version"
                    {:code :integrity/unsupported-version
                     :manifest-version (:version manifest)
                     :expected-version backup-manifest-version})))
  (let [sorted-data (into (sorted-map) data)
        collection-counts (into {} (map (fn [[k v]] [k (count v)]) sorted-data))]
    (when (:collections manifest)
      (when-not (= (set (keys (:collections manifest))) (set (keys sorted-data)))
        (throw (ex-info "Backup manifest collections do not match payload collections"
                        {:code :integrity/corrupt
                         :manifest-collections (sort (keys (:collections manifest)))
                         :payload-collections (sort (keys sorted-data))})))
      (when-not (= (:collections manifest) collection-counts)
        (throw (ex-info "Backup manifest counts do not match payload counts"
                        {:code :integrity/corrupt
                         :manifest-counts (:collections manifest)
                         :payload-counts collection-counts}))))
    (when (:content-hash manifest)
      (let [actual-hash (sha256-base64 (pr-str sorted-data))]
        (when-not (= (:content-hash manifest) actual-hash)
          (throw (ex-info "Backup content hash mismatch -- file may be corrupted or hand-edited"
                          {:code :integrity/corrupt
                           :expected-hash (:content-hash manifest)
                           :actual-hash actual-hash})))))
    (doseq [[collection-key records] sorted-data
            record records]
      (validate-record collection-key record))
    payload))

(defn- read-backup-file
  "Read and EDN-parse a backup file. A missing file is :source/unavailable;
   an unparseable one is :integrity/corrupt. Never a bare reader exception."
  [file-path]
  (when-not (.exists (io/file file-path))
    (throw (ex-info (str "Backup file not found: " file-path)
                    {:code :source/unavailable
                     :file file-path})))
  (try
    (edn/read-string (slurp file-path))
    (catch Exception e
      (throw (ex-info (str "Backup file is not parseable EDN (truncated or corrupted): " file-path)
                      {:code :integrity/corrupt
                       :file file-path
                       :parse-error (.getMessage e)}
                      e)))))

(defn export-to-file
  "Export all observations from the observations port to an EDN file.
   Returns a manifest map with :file, :manifest, :collection-counts, :total-docs."
  [observations-adapter file-path]
  (let [data (into (sorted-map) ((:export-all observations-adapter)))
        collection-counts (into {} (map (fn [[k v]] [k (count v)]) data))
        total-docs (apply + (vals collection-counts))
        content (pr-str data)
        manifest {:version      backup-manifest-version
                  :format       :epiphany-backup-v1
                  :collections  collection-counts
                  :total-docs   total-docs
                  :content-hash (sha256-base64 content)}
        payload  {:manifest manifest :data data}]
    (io/make-parents (io/file file-path))
    (spit file-path (pr-str payload))
    {:file          file-path
     :manifest      manifest
     :collection-counts collection-counts
     :total-docs    total-docs}))

(defn import-from-file
  "Import observations from an EDN backup file into the observations port.
   Validates the ENTIRE payload — parseability, format, version, manifest
   collections + counts, content-hash, and every record against its
   schema — BEFORE any mutation: a corrupted backup mutates nothing and
   is reported with a named integrity category. Returns a map of
   collection names to imported counts."
  [observations-adapter file-path]
  (let [payload (read-backup-file file-path)
        {:keys [manifest data]} (validate-backup-payload payload)]
    ((:import-all observations-adapter) data)
    (let [imported-counts (into {} (map (fn [[k v]] [k (count v)]) data))]
      (when (and (:collections manifest)
                 (not= (:collections manifest) imported-counts))
        (throw (ex-info "Imported collection counts do not match backup manifest"
                        {:code :integrity/corrupt
                         :manifest-counts (:collections manifest)
                         :imported-counts imported-counts})))
      imported-counts)))

(defn inaccessible-sources
  "Check which repository paths from a backup are no longer accessible.
   Returns a vector of {:path raw, :resource-id uuid, :reason string}."
  [git-adapter backup-data]
  (let [repo-locations (get backup-data "repository-location" [])]
    (reduce
     (fn [acc observation]
       (let [repo-path (get-in observation [:repository/path :path/raw])
             resource-id (:resource-id observation)]
         (try
           (let [common-dir ((:common-git-directory git-adapter) repo-path)]
             (if (and common-dir (.isDirectory (io/file common-dir)))
               acc
               (conj acc {:path repo-path
                          :resource-id resource-id
                          :reason "common-git-dir-not-directory"})))
           (catch Exception _e
             (conj acc {:path repo-path
                        :resource-id resource-id
                        :reason "repository-not-found"})))))
     []
     repo-locations)))

(defn restore-drill
  "Execute a full backup/restore drill against a live observations port --
   verified by actually running every stage, not asserted:

     1. Export the current store to file.
     2. Drop all observation data via the port's :clear-all! op, simulating
        cache/index/store loss.
     3. Import from the exported file back into the (now-empty) port.
     4. Re-export and compare against the original export's content-hash --
        confirms the restore round-trips byte-identically, not merely
        'some data landed'.
     5. Check the restored repository-location observations for
        inaccessible Git sources -- recorded explicitly in the report,
        never papered over as a clean restore.

   Returns a drill report map:
     {:export {...}, :import {...}, :re-export {...}
      :round-trip-identical? bool
      :inaccessible-sources [...]
      :drill-status :complete | :round-trip-mismatch}"
  [observations-adapter git-adapter backup-dir]
  (let [backup-file (str backup-dir "/backup.edn")
        re-export-file (str backup-dir "/backup-re-export.edn")

        export-result (export-to-file observations-adapter backup-file)
        _ ((:clear-all! observations-adapter))
        import-result (import-from-file observations-adapter backup-file)
        re-export-result (export-to-file observations-adapter re-export-file)
        round-trip-identical? (= (:content-hash (:manifest export-result))
                                  (:content-hash (:manifest re-export-result)))
        restored-data (:data (edn/read-string (slurp backup-file)))
        inaccessible (inaccessible-sources git-adapter restored-data)]
    {:export export-result
     :import import-result
     :re-export re-export-result
     :round-trip-identical? round-trip-identical?
     :inaccessible-sources inaccessible
     :drill-status (if round-trip-identical? :complete :round-trip-mismatch)}))
