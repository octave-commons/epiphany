(ns epiphany.extern.backup-files
  "Filesystem ownership for a serialized, immutable restore snapshot."
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.edn :as edn])
  (:import [java.nio.channels FileChannel]
           [java.nio.file FileAlreadyExistsException NoSuchFileException OpenOption Paths StandardOpenOption]))

(defn with-lock! [directory operation]
  (fs/ensure-dir! directory)
  (let [path (str directory "/restore.lock")]
    (try (fs/create-exclusive! path)
         (catch FileAlreadyExistsException _))
    (let [lock (fs/acquire-lock! path)]
      (try (operation) (finally (fs/release-lock! lock))))))

(defn read-existing
  "Return an explicit payload envelope when present, including EDN nil.
   Only an actual missing file is absent; failed reads remain named errors."
  [file]
  (try
    {:payload (edn/read-string (fs/read-text file))}
    (catch NoSuchFileException _ nil)
    (catch java.io.IOException cause
      (throw (ex-info "Restore backup cannot be read"
                      {:code :source/unavailable :file file} cause)))
    (catch Exception cause
      (throw (ex-info "Restore backup is not parseable EDN"
                      {:code :integrity/corrupt :file file} cause)))))

(defn ensure-durable! [directory file]
  (with-open [channel (FileChannel/open
                       (Paths/get file (make-array String 0))
                       (into-array OpenOption [StandardOpenOption/WRITE]))]
    (fs/force-file! channel))
  (fs/sync-directory! directory)
  file)

(defn publish-new!
  "Publish a complete forced file through an exclusive hard link, then fence it.
   An existing destination is never replaced, even if another caller wins."
  [directory file payload]
  (let [temporary (str directory "/restore-snapshot-" (random-uuid) ".tmp")]
    (try
      (fs/create-exclusive! temporary)
      (fs/write-text! temporary (pr-str payload))
      (fs/hard-link! temporary file)
      (ensure-durable! directory file)
      (finally (fs/delete-if-exists! temporary))))
  payload)
