(ns epiphany.infra.backup
  "Restore effect orchestration. Each identified drill owns one backup directory."
  (:require [epiphany.domain.backup :as backup]
            [epiphany.extern.backup-files :as files]))

(defn- export-result [file {:keys [manifest]}]
  {:file file :manifest manifest
   :collection-counts (:collections manifest) :total-docs (:total-docs manifest)})

(defn- retained-backup! [observations directory file command-id]
  (let [existing (files/read-existing file)
        payload (if existing
                  (:payload existing)
                  (assoc (backup/export-payload ((:export-all observations)))
                         :restore/command-id command-id))]
    (backup/validate-restore-backup payload command-id)
    (if existing
      (files/ensure-durable! directory file)
      (files/publish-new! directory file payload))
    (export-result file payload)))

(defn- initial-export! [observations directory file command-id]
  (if command-id
    (retained-backup! observations directory file command-id)
    (do
      (when-let [existing (files/read-existing file)]
        (backup/validate-legacy-restore-backup (:payload existing)))
      (backup/export-to-file observations file))))

(defn- run-drill! [observations git directory command-id]
  (let [file (str directory "/backup.edn")
        re-export-file (str directory "/backup-re-export.edn")
        exported (initial-export! observations directory file command-id)
        _ (apply (:clear-all! observations) (when command-id [command-id]))
        imported (backup/import-from-file observations file)
        re-exported (backup/export-to-file observations re-export-file)
        identical? (= (get-in exported [:manifest :content-hash])
                      (get-in re-exported [:manifest :content-hash]))
        restored-data (get-in (files/read-existing file) [:payload :data])]
    {:export exported :import imported :re-export re-exported
     :round-trip-identical? identical?
     :inaccessible-sources (backup/inaccessible-sources git restored-data)
     :drill-status (if identical? :complete :round-trip-mismatch)}))

(defn restore-drill
  "Export, clear, import, compare and report inaccessible sources.
   Four arguments require a UUID and retain the first snapshot durably before
   clearing. Retry with that UUID and directory; use a new directory for a new
   drill. Three arguments preserve legacy zero-argument clear behavior.
   Migrated from epiphany.domain.backup to keep new effects outside domain."
  ([observations git directory]
   (files/with-lock! directory #(run-drill! observations git directory nil)))
  ([observations git directory command-id]
   (when-not (uuid? command-id)
     (throw (ex-info "Identified restore requires a UUID"
                     {:code :restore/invalid-command :command-id command-id})))
   (files/with-lock! directory #(run-drill! observations git directory command-id))))
