(ns epiphany.infra.repository-metadata-file
  (:require [clojure.java.io :as io]
            [epiphany.domain.repository-identity :as repository-identity]))

(declare read!)

(defn metadata-path [common-git-dir]
  (repository-identity/repository-metadata-path common-git-dir))

(defn write! [common-git-dir resource-id]
  (let [path (metadata-path common-git-dir)
        file (io/file path)
        requested-metadata (repository-identity/repository-metadata resource-id)]
    (if (.exists file)
      (let [existing-metadata (read! common-git-dir)]
        (when-not (= existing-metadata requested-metadata)
          (throw (ex-info "Repository metadata resource ID conflicts with existing identity"
                          {:existing-resource-id (:resource-id existing-metadata)
                           :requested-resource-id resource-id
                           :path path})))
        path)
      (do
        (.mkdirs (.getParentFile file))
        (spit file (repository-identity/write-repository-metadata requested-metadata))
        path))))

(defn read! [common-git-dir]
  (repository-identity/read-repository-metadata
   (slurp (metadata-path common-git-dir))))
