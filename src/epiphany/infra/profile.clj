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
   :services  throws UNAVAILABLE — no adapter may silently substitute."
  [{:keys [profile common-git-dir-fn]}]
  (case profile
    :local
    (in-memory/make {:common-git-dir-fn common-git-dir-fn})

    :services
    (throw (ex-info (str "Profile :services is not yet available. "
                         "Start local services and use US-000C / ENG-001A adapters.")
                    {:code :unavailable
                     :profile :services
                     :hint "MongoDB/S3 adapters arrive with ENG-001A. Use :local for direct-mode testing."}))))

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
     Throws UNAVAILABLE — real service adapters arrive with US-000C and
     ENG-001A. This is intentional: the profile contract exists before
     any real adapter, and no adapter may silently substitute.

   Options:
     :profile            keyword — :local or :services
     :common-git-dir-fn  (fn [path] -> string) — required for :local"
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
