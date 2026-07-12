(ns epiphany.infra.profile
  "Profile contract and adapter resolution.

  Two explicit modes:
    :local     — in-process/direct mode. Uses in-memory adapters.
                 No external services required.
    :services  — locally provisioned adapters (MongoDB, S3, etc.).
                 Fails with UNAVAILABLE if a required service is unreachable.

  No profile silently falls back to another. Selection is explicit and
  visible in diagnostics and command output."

  (:require [epiphany.infra.adapters.in-memory :as in-memory]))

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

(defn resolve-adapters
  "Resolve a complete port map for the given profile.

   :local
     Returns in-memory adapters. Requires :common-git-dir-fn in opts
     (the function that resolves a path to its common Git directory).

   :services
     Throws UNAVAILABLE — real service adapters arrive with US-000C and
     ENG-001A. This is intentional: the profile contract exists before
     any real adapter, and no adapter may silently substitute.

   Options:
     :profile            keyword — :local or :services
     :common-git-dir-fn  (fn [path] -> string) — required for :local"
  [{:keys [profile common-git-dir-fn]}]
  (validate-profile! profile)
  (case profile
    :local
    (in-memory/make {:common-git-dir-fn common-git-dir-fn})

    :services
    (throw (ex-info (str "Profile :services is not yet available. "
                         "Start local services and use US-000C / ENG-001A adapters.")
                    {:code :unavailable
                     :profile :services
                     :hint "MongoDB/S3 adapters arrive with ENG-001A. Use :local for direct-mode testing."}))))

;; ---------------------------------------------------------------------------
;; Diagnostics

(defn profile-description
  "Human-readable description of a profile for CLI output and diagnostics."
  [profile]
  (validate-profile! profile)
  (case profile
    :local    "local (in-memory, no external services)"
    :services "services (locally provisioned MongoDB, S3, etc.)"))
