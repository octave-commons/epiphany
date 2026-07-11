(ns epiphany.application.registration
  (:require [epiphany.domain.repository-identity :as repository-identity]))

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
              result {:resource-id resource-id
                      :repository-path repository-path
                      :common-git-dir common-git-dir
                      :request-id request-id}]
          (when-not existing-metadata
            ((:write repository-metadata) common-git-dir resource-id))
          ((:record-repository-location! observations)
           (cond-> result
             (nil? request-id) (dissoc :request-id)))
          result))))
