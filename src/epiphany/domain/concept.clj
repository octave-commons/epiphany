(ns epiphany.domain.concept
  "User-created concepts and research-question records anchored to
  selected section expressions.

  Concepts are user-curated labels for ideas found in the corpus.
  Research questions record the user's interpretation plus linked
  evidence — they are not objective claims.

  Both serve as search and trace entry points. Removing evidence from
  a concept deletes nothing at the source or review layer.")

;; ---------------------------------------------------------------------------
;; Concepts

(defn make-concept
  "Create a concept record.

   Parameters:
     name — string, the concept name
     options — optional map:
       :description string — optional description
       :evidence-links [map] — initial evidence links (each with :path-raw, :commit-oid, :heading-path)
       :created-by string — who created it (default \"user\")
       :tags [string] — optional tags for categorization

   Returns a concept map."
  [name & {:keys [description evidence-links created-by tags]
           :or {evidence-links [] created-by "user" tags []}}]
  (assert (and name (seq name)) "Concept name is required")
  {:concept/id (java.util.UUID/randomUUID)
   :concept/name name
   :concept/description description
   :concept/evidence-links (vec evidence-links)
   :concept/created-at (java.util.Date.)
   :concept/created-by created-by
   :concept/tags (vec tags)
   :concept/type :concept})

(defn add-evidence
  "Add an evidence link to a concept. Returns updated concept."
  [concept evidence-link]
  (update concept :concept/evidence-links conj evidence-link))

(defn remove-evidence
  "Remove an evidence link from a concept by path+heading+commit.
   Returns updated concept. Does not affect source or review layer."
  [concept path-raw heading-path commit-oid]
  (update concept :concept/evidence-links
          (fn [links]
            (vec (remove (fn [link]
                       (and (= path-raw (:path-raw link))
                            (= heading-path (:heading-path link))
                            (= commit-oid (:commit-oid link))))
                      links)))))

(defn add-tag
  "Add a tag to a concept. Returns updated concept."
  [concept tag]
  (update concept :concept/tags conj tag))

(defn remove-tag
  "Remove a tag from a concept. Returns updated concept."
  [concept tag]
   (update concept :concept/tags (fn [tags] (vec (remove #(= % tag) tags)))))

(defn search-entry-point
  "Return a map suitable for use as a search entry point.
   Includes the concept's name, description, and evidence paths."
  [concept]
  {:entry/type :concept
   :entry/id (:concept/id concept)
   :entry/name (:concept/name concept)
   :entry/description (:concept/description concept)
   :entry/tags (:concept/tags concept)
   :entry/evidence-paths (mapv :path-raw (:concept/evidence-links concept))})

;; ---------------------------------------------------------------------------
;; Research questions

(defn make-research-question
  "Create a research question record.

   A research question records the user's interpretation plus linked
   evidence — it is not an objective claim.

   Parameters:
     question — string, the research question
     options — optional map:
       :interpretation string — user's interpretation or hypothesis
       :evidence-links [map] — initial evidence links
       :created-by string — who created it (default \"user\")
       :priority keyword — :high, :medium, :low (default :medium)
       :status keyword — :open, :investigating, :answered, :dismissed (default :open)

   Returns a research question map."
  [question & {:keys [interpretation evidence-links created-by priority status]
               :or {evidence-links [] created-by "user" priority :medium status :open}}]
  (assert (and question (seq question)) "Question is required")
  {:research-question/id (java.util.UUID/randomUUID)
   :research-question/question question
   :research-question/interpretation interpretation
   :research-question/evidence-links (vec evidence-links)
   :research-question/created-at (java.util.Date.)
   :research-question/created-by created-by
   :research-question/priority priority
   :research-question/status status
   :research-question/type :research-question})

(defn answer-question
  "Mark a research question as answered with a note."
  [rq answer-note]
  (-> rq
      (assoc :research-question/status :answered)
      (assoc :research-question/answer answer-note)
      (assoc :research-question/answered-at (java.util.Date.))))

(defn dismiss-question
  "Dismiss a research question with a reason."
  [rq reason]
  (-> rq
      (assoc :research-question/status :dismissed)
      (assoc :research-question/dismiss-reason reason)
      (assoc :research-question/dismissed-at (java.util.Date.))))

(defn rq-add-evidence
  "Add an evidence link to a research question. Returns updated record."
  [rq evidence-link]
  (update rq :research-question/evidence-links conj evidence-link))

(defn rq-remove-evidence
  "Remove an evidence link from a research question."
  [rq path-raw heading-path commit-oid]
  (update rq :research-question/evidence-links
          (fn [links]
            (vec (remove (fn [link]
                       (and (= path-raw (:path-raw link))
                            (= heading-path (:heading-path link))
                            (= commit-oid (:commit-oid link))))
                      links)))))

(defn rq-search-entry-point
  "Return a map suitable for use as a search entry point."
  [rq]
  {:entry/type :research-question
   :entry/id (:research-question/id rq)
   :entry/name (:research-question/question rq)
   :entry/description (:research-question/interpretation rq)
   :entry/status (:research-question/status rq)
   :entry/priority (:research-question/priority rq)
   :entry/evidence-paths (mapv :path-raw (:research-question/evidence-links rq))})

;; ---------------------------------------------------------------------------
;; Collections

(defn list-concepts
  "Filter a collection of concepts by optional criteria."
  [concepts & {:keys [tag name-substring]}]
  (cond->> concepts
    tag (filter #(contains? (set (:concept/tags %)) tag))
    name-substring (filter #(clojure.string/includes?
                            (clojure.string/lower-case (:concept/name %))
                            (clojure.string/lower-case name-substring)))))

(defn list-research-questions
  "Filter a collection of research questions by optional criteria."
  [rqs & {:keys [status priority]}]
  (cond->> rqs
    status (filter #(= status (:research-question/status %)))
    priority (filter #(= priority (:research-question/priority %)))))
