(ns epiphany.application.registration
  (:require [epiphany.domain.repository-identity :as repository-identity]
            [epiphany.law.registry :as registry]))

(defn- path-observed
  "Wrap a raw path string into a path/observed provenance map."
  [path]
  {:path/raw path
   :path/source :filesystem-argument
   :path/comparison :exact})

(defn- replay-registration
  "Refuse changed intent and reaffirm durability before acknowledging a retry."
  [observations {:keys [request-id repository-path]} existing]
  (when-not (= repository-path (get-in existing [:repository/path :path/raw]))
    (throw (ex-info "Registration request-id was already used for a different repository path"
                    {:code :bad-request :reason :idempotency-conflict})))
  ((:record-repository-location! observations) existing)
  {:resource-id (:resource-id existing)
   :repository-path repository-path
   :common-git-dir (get-in existing [:repository/common-git-dir :path/raw])
   :request-id request-id})

(defn register!
  "Register an explicitly identified command; invalid calls reach no port."
  [{:keys [git repository-metadata observations]} command]
  (when-not (and (map? command)
                 (registry/valid? "command/register"
                                  (merge {:command/name :command/register} command)))
    (throw (ex-info "Registration requires a command map, repository-path and UUID request-id"
                    {:code :bad-request})))
  (let [{:keys [request-id repository-path]} command]
    (or (when-let [existing ((:find-by-request-id observations) request-id)]
          (replay-registration observations command existing))
        (let [common-git-dir ((:common-git-directory git) repository-path)
              existing-metadata ((:read repository-metadata) common-git-dir)
              resource-id (or (:resource-id existing-metadata)
                              (repository-identity/new-resource-id))
              observation {:observation/type :repository/location-observed
                           :observation/id (random-uuid)
                           :observation/request-id request-id
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
