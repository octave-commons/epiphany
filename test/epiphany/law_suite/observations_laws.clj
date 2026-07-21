(ns epiphany.law-suite.observations-laws
  "Parameterized law suite for observation-port adapters.

  Every adapter that implements the observations port must pass this
  suite. The harness is data-parameterized: supply either a factory
  function (`:make-port`) or a concrete `:port`, plus a `:capabilities`
  declaration, and the laws judge the adapter by identical criteria.

  The runner is a pure function of its argument map: it returns a
  normalized map of `law-name -> outcome` and emits no `clojure.test`
  assertions itself. Callers inspect the returned data and make their
  own assertions. This lets a negative fixture prove the harness has
  teeth (i.e. that a permissive adapter genuinely FAILS the rejection
  laws) without failing the enclosing test suite.

  Each outcome is a map:
    {:outcome :pass}                       ; law held
    {:outcome :fail   :detail \"...\"}      ; law violated
    {:outcome :skip   :capability :kw}     ; capability not declared

  `:skip` and `:pass` are genuinely distinguishable — a law whose
  required capability is not declared is reported as `:skip`, never as
  a silent pass.

  A fresh port is drawn per law (via `:make-port`) so laws do not
  contaminate one another's state; when only a shared `:port` is given
  every law runs against that same instance.

  Usage:
    (observations-laws {:make-port   (fn [] (:observations (in-memory/make ...)))
                        :capabilities #{:schema-validation :idempotency :export-import}})")

;; ---------------------------------------------------------------------------
;; Fixture builders

(defn- valid-repository-location
  "Build a valid observation/repository-location-v1 record."
  [rid]
  {:observation/id #uuid "00000000-0000-0000-0000-000000000001"
   :observation/observed-at #inst "2026-01-01T00:00:00.000Z"
   :observation/adapter-version "law-suite-v1"
   :observation/schema-version 1
   :observation/type :repository/location-observed
   :observation/request-id rid
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :repository/path {:path/raw "/law/test-repo"
                     :path/source :filesystem-argument
                     :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/law/test-repo/.git"
                               :path/source :filesystem-argument
                               :path/comparison :exact}})

(defn- invalid-record
  "A map that fails every observation schema (missing envelope)."
  []
  {:observation/request-id #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"})

(defn- second-valid-repository-location
  "A valid record with a different observation/id for conflict tests."
  [rid]
  (assoc (valid-repository-location rid)
         :observation/id #uuid "00000000-0000-0000-0000-000000000002"))

;; ---------------------------------------------------------------------------
;; Law definitions
;;
;; Each law is a pure predicate over a fresh port: it returns an outcome
;; map ({:outcome :pass} or {:outcome :fail :detail "..."}) and NEVER
;; emits a clojure.test assertion. That is what lets the negative
;; fixture assert "these laws failed" without failing the suite.

(defn- pass [] {:outcome :pass})
(defn- fail [detail] {:outcome :fail :detail detail})

(defn- law-valid-write-accepted
  "A valid record is accepted without error (returns nil)."
  [port]
  (let [rid #uuid "10000000-0000-0000-0000-000000000001"
        record (valid-repository-location rid)]
    (try
      (let [result ((:record-repository-location! port) record)]
        (if (nil? result)
          (pass)
          (fail (str "expected nil for a valid first write, got " (pr-str result)))))
      (catch Exception e
        (fail (str "valid write threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-invalid-write-rejected
  "An invalid record is rejected with ExceptionInfo."
  [port]
  (try
    (let [result ((:record-repository-location! port) (invalid-record))]
      (fail (str "invalid record was accepted (no ExceptionInfo); returned "
                 (pr-str result))))
    (catch clojure.lang.ExceptionInfo _
      (pass))
    (catch Exception e
      (fail (str "invalid write threw " (.getName (class e))
                 ", expected clojure.lang.ExceptionInfo")))))

(defn- law-rejection-leaves-state-unchanged
  "A rejected write leaves export-all byte-identical."
  [port]
  (let [snapshot-before ((:export-all port))]
    (try
      ((:record-repository-location! port) (invalid-record))
      (catch clojure.lang.ExceptionInfo _)
      (catch Exception _))
    (let [snapshot-after ((:export-all port))]
      (if (= snapshot-before snapshot-after)
        (pass)
        (fail "export-all differs before vs after a rejected write")))))

(defn- law-idempotent-replay-stable
  "Same request-ID with identical content replays without mutation."
  [port]
  (let [rid #uuid "20000000-0000-0000-0000-000000000001"
        record (valid-repository-location rid)]
    (try
      ((:record-repository-location! port) record)
      (let [result ((:record-repository-location! port) record)]
        (cond
          (some? result)
          (fail (str "replay with identical content must return nil, got " (pr-str result)))
          (not= record ((:find-by-request-id port) rid))
          (fail "stored fact changed after an identical replay")
          :else (pass)))
      (catch Exception e
        (fail (str "idempotent replay threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-changed-content-replay-conflicts
  "Same request-ID with different content returns :idempotency-conflict."
  [port]
  (let [rid #uuid "30000000-0000-0000-0000-000000000001"
        record1 (valid-repository-location rid)
        record2 (second-valid-repository-location rid)]
    (try
      ((:record-repository-location! port) record1)
      (let [result ((:record-repository-location! port) record2)]
        (cond
          (not= :idempotency-conflict (:code result))
          (fail (str "expected {:code :idempotency-conflict}, got " (pr-str result)))
          (not= rid (:request-id result))
          (fail "conflict result must include the offending request-id")
          (not= record1 ((:find-by-request-id port) rid))
          (fail "stored fact must remain the original, not the conflicting one")
          :else (pass)))
      (catch Exception e
        (fail (str "changed-content replay threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-export-import-round-trip
  "Export then import preserves all data."
  [port]
  (let [rid #uuid "40000000-0000-0000-0000-000000000001"
        record (valid-repository-location rid)]
    (try
      ((:record-repository-location! port) record)
      (let [exported ((:export-all port))]
        ((:import-all port) exported)
        (if (= (get exported "repository-location")
               (get ((:export-all port)) "repository-location"))
          (pass)
          (fail "re-export after import does not match original export")))
      (catch Exception e
        (fail (str "export/import round-trip threw " (.getName (class e)) ": " (.getMessage e)))))))

;; ---------------------------------------------------------------------------
;; Law registry
;;
;; Declarative: each entry names the law, the capability it requires
;; (nil = applies to every adapter), and the predicate that judges it.
;; Gating lives here, so "which laws are capability-gated" is data, not
;; control flow buried in the runner. In particular the export/import
;; round-trip is GATED on :export-import and only runs when declared.

(def ^:private law-registry
  [{:law :valid-write-accepted            :capability nil                :run law-valid-write-accepted}
   {:law :invalid-write-rejected          :capability :schema-validation :run law-invalid-write-rejected}
   {:law :rejection-leaves-state-unchanged :capability :schema-validation :run law-rejection-leaves-state-unchanged}
   {:law :idempotent-replay-stable        :capability :idempotency       :run law-idempotent-replay-stable}
   {:law :changed-content-replay-conflicts :capability :idempotency       :run law-changed-content-replay-conflicts}
   {:law :export-import-round-trip        :capability :export-import      :run law-export-import-round-trip}])

(defn- resolve-port-provider
  "Return a zero-arg provider that yields a port for a single law run.
  Prefers `:make-port` (fresh, isolated per law); falls back to a shared
  `:port`."
  [{:keys [make-port port]}]
  (cond
    make-port make-port
    port (constantly port)
    :else (throw (ex-info "law suite requires :make-port or :port"
                          {:code :invalid-law-suite-argument}))))

;; ---------------------------------------------------------------------------
;; Law suite runner

(defn observations-laws
  "Run the observation-port law suite and return normalized outcomes.

  Argument map:
    :make-port    — zero-arg factory returning a fresh observations port
                    (preferred: each law runs against an isolated port).
    :port         — a shared observations port map (used only when
                    :make-port is absent).
    :capabilities — a set of capability keywords declared by the adapter.

  Returns a map of law-keyword -> outcome map. Every registered law is
  present in the result:
    {:outcome :pass}
    {:outcome :fail :detail \"...\"}
    {:outcome :skip :capability :kw}   ; required capability not declared

  Supported capabilities:
    :schema-validation — adapter validates records against schemas
    :idempotency      — adapter enforces request-ID idempotency
    :export-import    — adapter supports export-all/import-all round-trip

  This function emits no clojure.test assertions; callers inspect the
  returned data. Skip and pass are distinguishable outcomes."
  [{:keys [capabilities] :as args}]
  (let [caps (or capabilities #{})
        provider (resolve-port-provider args)]
    (into {}
          (for [{:keys [law capability run]} law-registry]
            [law
             (if (and capability (not (contains? caps capability)))
               {:outcome :skip :capability capability}
               (run (provider)))]))))

(defn failed-laws
  "Return the set of law keywords whose outcome is :fail."
  [outcomes]
  (into #{} (keep (fn [[law {:keys [outcome]}]]
                    (when (= :fail outcome) law))
                  outcomes)))

(defn skipped-laws
  "Return the set of law keywords whose outcome is :skip."
  [outcomes]
  (into #{} (keep (fn [[law {:keys [outcome]}]]
                    (when (= :skip outcome) law))
                  outcomes)))
