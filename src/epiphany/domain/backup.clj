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
  ;; :collections and :content-hash are REQUIRED, not opt-out — a
  ;; manifest missing either is itself evidence of tampering (no
  ;; permissive legacy branch).
  (when-not (map? (:collections manifest))
    (throw (ex-info "Backup manifest is missing its :collections counts"
                    {:code :integrity/corrupt
                     :manifest-keys (sort (keys manifest))})))
  (when-not (string? (:content-hash manifest))
    (throw (ex-info "Backup manifest is missing its :content-hash"
                    {:code :integrity/corrupt
                     :manifest-keys (sort (keys manifest))})))
  (let [sorted-data (into (sorted-map) data)
        collection-counts (into {} (map (fn [[k v]] [k (count v)]) sorted-data))]
    (when-not (= (set (keys (:collections manifest))) (set (keys sorted-data)))
      (throw (ex-info "Backup manifest collections do not match payload collections"
                      {:code :integrity/corrupt
                       :manifest-collections (sort (keys (:collections manifest)))
                       :payload-collections (sort (keys sorted-data))})))
    (when-not (= (:collections manifest) collection-counts)
      (throw (ex-info "Backup manifest counts do not match payload counts"
                      {:code :integrity/corrupt
                       :manifest-counts (:collections manifest)
                       :payload-counts collection-counts})))
    (let [actual-hash (sha256-base64 (pr-str sorted-data))]
      (when-not (= (:content-hash manifest) actual-hash)
        (throw (ex-info "Backup content hash mismatch -- file may be corrupted or hand-edited"
                        {:code :integrity/corrupt
                         :expected-hash (:content-hash manifest)
                         :actual-hash actual-hash}))))
    (doseq [[collection-key records] sorted-data
            record records]
      (validate-record collection-key record))
    payload))

(defn validate-restore-backup
  "An identified restore may only reuse the snapshot bound to its command."
  [payload command-id]
  (validate-backup-payload payload)
  (when-not (= command-id (:restore/command-id payload))
    (throw (ex-info "Restore directory belongs to a different or unidentified command"
                    {:code :restore/command-conflict
                     :command-id command-id
                     :backup-command-id (:restore/command-id payload)})))
  payload)

(defn validate-legacy-restore-backup
  "Legacy drills cannot replace an identified command's retained snapshot."
  [payload]
  (validate-backup-payload payload)
  (when (:restore/command-id payload)
    (throw (ex-info "An unidentified drill cannot replace an identified backup"
                    {:code :restore/command-conflict})))
  payload)

(defn- read-backup-file
  "Read and EDN-parse a backup file. A missing or unreadable file is
   :source/unavailable; an unparseable one is :integrity/corrupt.
   Never a bare reader exception."
  [file-path]
  (let [file (io/file file-path)]
    (when-not (.exists file)
      (throw (ex-info (str "Backup file not found: " file-path)
                      {:code :source/unavailable
                       :file file-path})))
    (when-not (.canRead file)
      (throw (ex-info (str "Backup file is not readable: " file-path)
                      {:code :source/unavailable
                       :file file-path})))
    (try
      (edn/read-string (slurp file))
      (catch java.io.IOException e
        (throw (ex-info (str "Backup file could not be read: " file-path)
                        {:code :source/unavailable
                         :file file-path
                         :io-error (.getMessage e)}
                        e)))
      (catch Exception e
        (throw (ex-info (str "Backup file is not parseable EDN (truncated or corrupted): " file-path)
                        {:code :integrity/corrupt
                         :file file-path
                         :parse-error (.getMessage e)}
                        e))))))

(defn export-payload
  "Construct the existing backup manifest and data from an observation snapshot."
  [snapshot]
  (let [data (into (sorted-map) snapshot)
        collection-counts (into {} (map (fn [[k v]] [k (count v)]) data))
        total-docs (apply + (vals collection-counts))
        content (pr-str data)
        manifest {:version      backup-manifest-version
                  :format       :epiphany-backup-v1
                  :collections  collection-counts
                  :total-docs   total-docs
                  :content-hash (sha256-base64 content)}
        payload {:manifest manifest :data data}]
    payload))

(defn export-to-file
  "Export all observations from the observations port to an EDN file.
   Returns a manifest map with :file, :manifest, :collection-counts, :total-docs."
  [observations-adapter file-path]
  (let [{:keys [manifest] :as payload}
        (export-payload ((:export-all observations-adapter)))]
    (io/make-parents (io/file file-path))
    (spit file-path (pr-str payload))
    {:file          file-path
     :manifest      manifest
     :collection-counts (:collections manifest)
     :total-docs    (:total-docs manifest)}))

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
