(ns epiphany.infra.profile
  "Profile contract and adapter resolution.

  Two explicit modes:
    :local     — in-process/direct mode. Uses in-memory adapters.
                 No external services required.
    :services  — locally provisioned adapters (MongoDB, S3, etc.).
                 Fails with UNAVAILABLE if a required service is unreachable.

  No profile silently falls back to another. Selection is explicit and
  visible in diagnostics and command output."

  (:require [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.adapters.lucene :as lucene]
            [epiphany.infra.adapters.mongo :as mongo]
            [epiphany.infra.adapters.ollama :as ollama]
            [epiphany.infra.repository-metadata-file :as repository-metadata-file]
            [epiphany.application.validation :as validation]))

(def valid-profiles
  "Set of recognized profile keywords."
  #{:local :services})

(defn valid-profile? [profile]
  (contains? valid-profiles profile))

(defn- validate-profile! [profile]
  (when-not (valid-profile? profile)
    (throw (ex-info (str "Unknown profile: " (pr-str profile)
                         ". Valid profiles: " (pr-str valid-profiles))
                    {:profile profile
                     :valid-profiles valid-profiles}))))

;; ---------------------------------------------------------------------------
;; Adapter resolution

(defn resolve-raw-adapters
  "Per-profile raw adapter map, BEFORE the validation wrapper is
  applied. Kept separate from `resolve-adapters` so the validation
  wrapping is profile-agnostic and cannot be forgotten on any one
  branch (see ENG-017B).

   :local     returns in-memory adapters (requires :common-git-dir-fn).
   :services  returns real adapters (MongoDB observations, Git-local
              repository.edn metadata, on-disk Lucene index, Ollama
              embeddings). Requires :mongo-conn (connection lifecycle
              stays with the caller) and :index-dir. Throws UNAVAILABLE
              when either is absent — no adapter may silently substitute."
  [{:keys [profile common-git-dir-fn mongo-conn index-dir]}]
  (case profile
    :local
    (in-memory/make {:common-git-dir-fn common-git-dir-fn})

    :services
    (do
      (when-not mongo-conn
        (throw (ex-info "Profile :services requires :mongo-conn."
                        {:code :unavailable
                         :profile :services
                         :hint "Connect via epiphany.infra.adapters.mongo/connect! and pass the connection in explicitly."})))
      (when-not index-dir
        (throw (ex-info "Profile :services requires :index-dir."
                        {:code :unavailable
                         :profile :services
                         :hint "Pass the durable Lucene index directory explicitly."})))
      {:git {:common-git-directory common-git-dir-fn}
       :repository-metadata {:read repository-metadata-file/read!
                             :write repository-metadata-file/write!
                             :list-repositories (fn [] [])}
       :observations (mongo/make-observations-adapter mongo-conn)
       :index (lucene/make-index-adapter
               {:index-dir (if (instance? java.nio.file.Path index-dir)
                             index-dir
                             (java.nio.file.Paths/get (str index-dir)
                                                      (into-array String [])))})
       :embeddings (ollama/make-embeddings-adapter {})})))

(defn resolve-adapters
  "Resolve a complete port map for the given profile.

   Every profile's observations port is wrapped by the ENG-017B
   validation gateway. The wrapping is applied here — outside the
   per-profile branch in `resolve-raw-adapters` — so NO profile
   (:local, :services, or any future one) can compose an unvalidated
   observations port.

    :local
      Returns in-memory adapters. Requires :common-git-dir-fn in opts
      (the function that resolves a path to its common Git directory).

    :services
      Returns real adapters (MongoDB, Lucene, Ollama). Requires
      :mongo-conn and :index-dir in opts. Unreachable/missing
      prerequisites are UNAVAILABLE, never a silent fallback.

   Options:
      :profile            keyword — :local or :services
      :common-git-dir-fn  (fn [path] -> string) — required
      :mongo-conn         Mongo connection map — required for :services
      :index-dir          Lucene index dir — required for :services"
  [{:keys [profile] :as opts}]
  (validate-profile! profile)
  (update (resolve-raw-adapters opts)
          :observations validation/validating-observations-port))

;; ---------------------------------------------------------------------------
;; Diagnostics

(defn profile-description
  "Human-readable description of a profile for CLI output and diagnostics."
  [profile]
  (validate-profile! profile)
  (case profile
    :local    "local (in-memory, no external services)"
    :services "services (locally provisioned MongoDB, S3, etc.)"))
