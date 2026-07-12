(ns epiphany.domain.history-replacement
  "History replacement detection.

   Detects when a previously observed ref target is no longer reachable
   and records it as an auditable observation. This does NOT conclude
   that history was rewritten — only that the ref now points to a
   different commit than previously recorded.

   Prior observations remain retained and queryable. Diagnostics clearly
   label the condition as observed ref/history evidence, not a semantic
   conclusion.")

;; ---------------------------------------------------------------------------
;; History replacement observation construction

(defn make-replacement-record
  "Construct a history-replacement observation map.

   Options:
     :resource-id       — the repository's resource-id (UUID)
     :ref-name          — the ref name string (e.g., \"refs/heads/main\")
     :old-oid           — the previously observed commit OID string
     :new-oid           — the newly observed commit OID string
     :old-observed-at   — timestamp of the old observation
     :new-observed-at   — timestamp of the new observation
     :observed-at       — observation envelope timestamp (default: now)
     :adapter-version   — adapter version string
     :schema-version    — schema version int (default: 1)
     :request-id        — optional request UUID"
  [{:keys [resource-id ref-name old-oid new-oid old-observed-at new-observed-at
           observed-at adapter-version schema-version request-id]}]
  (cond-> {:observation/type           :git/history-replacement-observed
           :observation/id             (java.util.UUID/randomUUID)
           :observation/observed-at    (or observed-at (java.util.Date.))
           :observation/adapter-version (or adapter-version "0.1.0")
           :observation/schema-version (or schema-version 1)
           :resource-id                resource-id
           :replacement/ref-name       ref-name
           :replacement/old-oid        old-oid
           :replacement/new-oid        new-oid
           :replacement/old-observed-at old-observed-at
           :replacement/new-observed-at new-observed-at}
    request-id
    (assoc :observation/request-id request-id)))

;; ---------------------------------------------------------------------------
;; Replacement detection

(defn detect-replacements
  "Compare current ref OID map against previously observed ref OID map.

   Returns a vector of replacement detection maps, each containing:
     :ref-name        — the ref name string
     :old-oid         — previously observed OID
     :new-oid         — currently observed OID
     :old-observed-at — timestamp of the old observation

   Only refs whose OID has changed are included. Refs present in
   current but not in previous are NOT flagged (they are new, not
   replacements). Refs present in previous but not in current are
   NOT flagged here — they are missing, which is a different condition."
  [previous-oid-map current-oid-map previous-timestamps]
  (into []
        (comp
         (filter
          (fn [[ref-name current-oid]]
            (let [previous-oid (get previous-oid-map ref-name)]
              (and previous-oid
                   (not= previous-oid current-oid)))))
         (map (fn [[ref-name new-oid]]
                {:ref-name        ref-name
                 :old-oid         (get previous-oid-map ref-name)
                 :new-oid         new-oid
                 :old-observed-at (get previous-timestamps ref-name)})))
        current-oid-map))

(defn detect-missing-refs
  "Detect refs that were previously observed but are no longer present.

   Returns a vector of maps with:
     :ref-name        — the ref name string
     :old-oid         — previously observed OID
     :old-observed-at — timestamp of the old observation

   These are distinct from replacements — the ref itself is gone,
   not just pointing to a different commit."
  [previous-oid-map current-oid-map previous-timestamps]
  (into []
        (map (fn [[ref-name old-oid]]
               {:ref-name        ref-name
                :old-oid         old-oid
                :old-observed-at (get previous-timestamps ref-name)}))
        (filter
         (fn [[ref-name _]]
           (not (contains? current-oid-map ref-name)))
         previous-oid-map)))
