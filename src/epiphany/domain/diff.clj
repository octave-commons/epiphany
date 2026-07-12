(ns epiphany.domain.diff
  "Pure domain service for comparing two historical evidence expressions.

  Given two section expressions, retrieves both evidence sets and produces
  a structured diff. No I/O — all Git access is injected via the :git port.

  The diff service never conflates continuity signals with the diff itself.
  Continuity information (same heading path, same blob) is displayed
  alongside but never treated as evidence of the diff."
  (:require [clojure.string :as str]
            [epiphany.domain.evidence :as evidence]))

;; ---------------------------------------------------------------------------
;; Line-level diffing

(defn- collect-lines-before-match
  "Walk seq-a looking for first element equal to target.
   Returns [skipped-entries remaining-a]."
  [seq-a target]
  (loop [skipped []
         remaining seq-a]
    (cond
      (empty? remaining) [skipped []]
      (= (first remaining) target) [skipped remaining]
      :else (recur (conj skipped (first remaining))
                   (rest remaining)))))

(defn- next-match-indices
  "Find where lines-a[i] appears in lines-b (and vice versa).
   Returns {:find-in-b int-or-nil, :find-in-a int-or-nil}."
  [lines-a i lines-b j]
  (let [find-in-b (loop [k j]
                    (when (< k (count lines-b))
                      (if (= (get lines-a i) (get lines-b k))
                        k
                        (recur (inc k)))))
        find-in-a (loop [k i]
                    (when (< k (count lines-a))
                      (if (= (get lines-b j) (get lines-a k))
                        k
                        (recur (inc k)))))]
    {:find-in-b find-in-b
     :find-in-a find-in-a}))

(defn compute-diff
  "Compute a line-level diff between two strings.

   Returns a vector of diff entries:
     {:type :equal/:delete/:insert
      :line-a int-or-nil (1-indexed)
      :line-b int-or-nil (1-indexed)
      :content string}"
  [content-a content-b]
  (let [lines-a (vec (str/split-lines content-a))
        lines-b (vec (str/split-lines content-b))]
    (loop [i 0
           j 0
           result []]
      (cond
        (and (>= i (count lines-a)) (>= j (count lines-b)))
        result

        (and (< i (count lines-a)) (< j (count lines-b))
             (= (get lines-a i) (get lines-b j)))
        (recur (inc i) (inc j)
               (conj result {:type :equal
                             :line-a (inc i)
                             :line-b (inc j)
                             :content (get lines-a i)}))

        (and (< i (count lines-a)) (< j (count lines-b)))
        (let [{:keys [find-in-b find-in-a]} (next-match-indices lines-a i lines-b j)
              dist-to-b (when find-in-b (- find-in-b j))
              dist-to-a (when find-in-a (- find-in-a i))]
          (cond
            (and dist-to-b dist-to-a (<= dist-to-b dist-to-a))
            (recur i (inc j)
                   (conj result {:type :insert
                                 :line-a nil
                                 :line-b (inc j)
                                 :content (get lines-b j)}))

            (and dist-to-a dist-to-b (> dist-to-b dist-to-a))
            (recur (inc i) j
                   (conj result {:type :delete
                                 :line-a (inc i)
                                 :line-b nil
                                 :content (get lines-a i)}))

            dist-to-b
            (recur i (inc j)
                   (conj result {:type :insert
                                 :line-a nil
                                 :line-b (inc j)
                                 :content (get lines-b j)}))

            dist-to-a
            (recur (inc i) j
                   (conj result {:type :delete
                                 :line-a (inc i)
                                 :line-b nil
                                 :content (get lines-a i)}))

            :else
            (recur (inc i) (inc j)
                   (-> result
                       (conj {:type :delete
                              :line-a (inc i)
                              :line-b nil
                              :content (get lines-a i)})
                       (conj {:type :insert
                              :line-a nil
                              :line-b (inc j)
                              :content (get lines-b j)})))))

        (< j (count lines-b))
        (recur i (inc j)
               (conj result {:type :insert
                             :line-a nil
                             :line-b (inc j)
                             :content (get lines-b j)}))

        :else
        (recur (inc i) j
               (conj result {:type :delete
                             :line-a (inc i)
                             :line-b nil
                             :content (get lines-a i)}))))))

;; ---------------------------------------------------------------------------
;; Diff formatting

(defn format-diff-text
  "Format a diff as human-readable unified text."
  [diff header-a header-b]
  (let [lines (concat
                [header-a header-b ""]
                (map (fn [{:keys [type content]}]
                       (case type
                         :equal (str "  " content)
                         :delete (str "- " content)
                         :insert (str "+ " content)))
                     diff))]
    (str/join "\n" lines)))

(defn format-diff-edn
  "Format a diff as EDN."
  [diff]
  (pr-str diff))

;; ---------------------------------------------------------------------------
;; Comparison service

(defn compare-evidence
  "Compare two evidence expressions and produce a structured diff.

   Parameters:
     ports   — application port map
     request — {:left  {:path string, :heading vector, :commit-oid string}
                :right {:path string, :heading vector, :commit-oid string}}

   Returns a map with:
     :diff/left-evidence   — left evidence result
     :diff/right-evidence  — right evidence result
     :diff/lines           — vector of diff entries
     :diff/summary         — {:equal int, :delete int, :insert int}
     :diff/continuity      — shared metadata (same path, same blob, etc.)
     :diff/failure         — non-nil if comparison failed"
  [ports {:keys [left right]}]
  (let [left-result  (evidence/retrieve-evidence ports left)
        right-result (evidence/retrieve-evidence ports right)]
    (cond
      (or (:evidence/unavailable left-result)
          (:evidence/unavailable right-result))
      {:diff/left-evidence left-result
       :diff/right-evidence right-result
       :diff/lines []
       :diff/summary {:equal 0 :delete 0 :insert 0}
       :diff/continuity {}
       :diff/failure (or (:evidence/failure left-result)
                         (:evidence/failure right-result))}

      :else
      (let [lines (compute-diff (:evidence/source left-result)
                                (:evidence/source right-result))
            summary (frequencies (map :type lines))
            continuity {:same-path (= (:evidence/path left-result)
                                      (:evidence/path right-result))
                        :same-heading (= (:evidence/heading-path left-result)
                                         (:evidence/heading-path right-result))
                        :same-blob (= (:evidence/commit-oid left-result)
                                      (:evidence/commit-oid right-result))}]
        {:diff/left-evidence left-result
         :diff/right-evidence right-result
         :diff/lines lines
         :diff/summary {:equal (get summary :equal 0)
                        :delete (get summary :delete 0)
                        :insert (get summary :insert 0)}
         :diff/continuity continuity
         :diff/failure nil}))))
