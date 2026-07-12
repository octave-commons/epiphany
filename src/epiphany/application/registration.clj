(ns epiphany.application.registration
  (:require [epiphany.domain.repository-identity :as repository-identity]))

(defn- path-observed
  "Wrap a raw path string into a path/observed provenance map."
  [path]
  {:path/raw path
   :path/source :filesystem-argument
   :path/comparison :exact})

(defn register! [{:keys [git repository-metadata observations]} command]
  (let [{:keys [request-id repository-path]} (if (map? command)
                                               command
                                               {:repository-path command})]
    (or (when request-id
          ((:find-by-request-id observations) request-id))
        (let [common-git-dir ((:common-git-directory git) repository-path)
              existing-metadata ((:read repository-metadata) common-git-dir)
              resource-id (or (:resource-id existing-metadata)
                              (repository-identity/new-resource-id))
              observation {:observation/type :repository/location-observed
                           :observation/id (random-uuid)
                           :observation/request-id (or request-id (random-uuid))
                           :observation/observed-at (java.util.Date.)
                           :observation/adapter-version "0.1.0"
                           :observation/schema-version 1
                           :resource-id resource-id
                           :repository/path (path-observed repository-path)
                           :repository/common-git-dir (path-observed common-git-dir)}]
          (when-not existing-metadata
            ((:write repository-metadata) common-git-dir resource-id))
          ((:record-repository-location! observations) observation)
          {:resource-id resource-id
           :repository-path repository-path
           :common-git-dir common-git-dir
           :request-id request-id}))))
