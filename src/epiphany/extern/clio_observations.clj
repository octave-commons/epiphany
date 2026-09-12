(ns epiphany.extern.clio-observations
  "Named JVM filesystem boundary for the Clio observation transaction lock."
  (:require [clio.extern.jvm.crypto :as crypto]
            [clio.extern.jvm.fs :as fs]
            [clio.infra.ledger :as ledger]
            [clio.shape.canonical :as canonical]
            [clojure.string :as string])
  (:import [java.nio.file FileAlreadyExistsException Paths]))

(defn state-hash
  "Hash the portable reference snapshot to detect semantic drift during replay."
  [state]
  (crypto/sha256 (canonical/canonical-edn state)))

(defn directory-path
  "Resolve an explicitly configured local directory; reject blank values."
  [directory]
  (when-not (and (string? directory) (not (string/blank? directory)))
    (throw (ex-info "EDN observations require an explicit directory"
                    {:code :unavailable :profile :edn})))
  (str (.normalize (.toAbsolutePath (Paths/get directory (make-array String 0))))))

(defn with-lock!
  "Serialize complete read/decision/append cycles on a separate lock inode."
  [directory f]
  (fs/ensure-dir! directory)
  (let [path (str directory "/observations.lock")]
    (try (fs/create-exclusive! path)
         (catch FileAlreadyExistsException _))
    (let [lock (fs/acquire-lock! path)]
      (try (f) (finally (fs/release-lock! lock))))))

(defn ensure-ledger!
  "Create only a new ledger; missing history beside known schemas is corruption."
  [directory]
  (let [file (str directory "/events.edn")]
    (when-not (fs/exists? file)
      (when (fs/exists? (str directory "/schemas"))
        (throw (ex-info "Observation ledger is missing beside known schemas"
                        {:code :integrity/missing-ledger :path file})))
      (ledger/create-ledger! file))
    file))

(defn configured-directory
  "Read the EDN storage directory from environment or the user's data directory."
  []
  (or (System/getenv "EPIPHANY_EDN_DIR")
      (str (System/getProperty "user.home") "/.epiphany/observations")))
