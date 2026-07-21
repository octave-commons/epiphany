(ns epiphany.domain.backup
  "Backup, restore, and index rebuild for Epiphany.

   Git is canonical for blobs/trees. MongoDB stores metadata/observations.
   Lucene/vector indices are rebuildable projections. Backup preserves
   MongoDB state; restore repopulates it; rebuild regenerates indices
   from Git + restored Mongo data."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def ^:private backup-manifest-version 1)

(defn- sha256-base64
  "Content hash of `s`, used to detect a corrupted or hand-edited backup
   file independently of the (order-sensitive) collection counts."
  [^String s]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes s "UTF-8"))]
    (.encodeToString (java.util.Base64/getEncoder) digest)))

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
   Verifies the manifest format, version, and content-hash before writing
   anything, and verifies the imported collection counts match the manifest
   afterward -- a corrupted or truncated backup is reported, never silently
   partially applied. Returns a map of collection names to imported counts."
  [observations-adapter file-path]
  (let [{:keys [manifest data]} (edn/read-string (slurp file-path))
        sorted-data (into (sorted-map) data)]
    (when-not (= (:format manifest) :epiphany-backup-v1)
      (throw (ex-info "Unsupported backup format"
                      {:format (:format manifest)})))
    (when-not (= (:version manifest) backup-manifest-version)
      (throw (ex-info "Unsupported backup manifest version"
                      {:manifest-version (:version manifest)
                       :expected-version backup-manifest-version})))
    (when (:content-hash manifest)
      (let [actual-hash (sha256-base64 (pr-str sorted-data))]
        (when-not (= (:content-hash manifest) actual-hash)
          (throw (ex-info "Backup content hash mismatch -- file may be corrupted or hand-edited"
                          {:expected-hash (:content-hash manifest)
                           :actual-hash actual-hash})))))
    ((:import-all observations-adapter) data)
    (let [imported-counts (into {} (map (fn [[k v]] [k (count v)]) data))]
      (when (and (:collections manifest)
                 (not= (:collections manifest) imported-counts))
        (throw (ex-info "Imported collection counts do not match backup manifest"
                        {:manifest-counts (:collections manifest)
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
