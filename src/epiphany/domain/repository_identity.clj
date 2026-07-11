(ns epiphany.domain.repository-identity
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn new-resource-id []
  (random-uuid))

(defn repository-metadata [resource-id]
  (when-not (uuid? resource-id)
    (throw (ex-info "Repository resource ID must be a UUID"
                    {:resource-id resource-id})))
  {:resource-id resource-id})

(defn valid-repository-metadata? [metadata]
  (and (map? metadata)
       (= #{:resource-id} (set (keys metadata)))
       (uuid? (:resource-id metadata))))

(defn write-repository-metadata [metadata]
  (when-not (valid-repository-metadata? metadata)
    (throw (ex-info "Repository metadata must contain only a UUID resource ID"
                    {:metadata metadata})))
  (pr-str metadata))

(defn read-repository-metadata [text]
  (let [metadata (edn/read-string text)]
    (when-not (valid-repository-metadata? metadata)
      (throw (ex-info "Repository metadata must contain only a UUID resource ID"
                      {:metadata metadata})))
    metadata))

(defn repository-metadata-path [common-git-dir]
  (str (io/file common-git-dir "corpus-archaeology" "repository.edn")))
