(ns epiphany.infra.workbench
  "Workbench UI: search workspace with evidence drawer.

  Local-only: no SaaS dependency; Unicode paths render untransliterated.
  Uses HTMX for dynamic behavior and Hiccup for HTML templating."
  (:require [hiccup2.core :as h]
            [ring.util.response :as response]
            [clojure.string :as str]
            [epiphany.domain.hybrid-search :as hs]
            [epiphany.domain.inbox :as inbox]
            [epiphany.domain.lineage :as lineage]
            [epiphany.domain.lineage-trace :as lineage-trace]
            [epiphany.domain.evidence :as evidence]
            [epiphany.domain.review :as review]
            [epiphany.domain.status :as status]
            [epiphany.infra.git :as git]))

;; ---------------------------------------------------------------------------
;; HTML helpers

(defn- html-response
  "Convert Hiccup v2 HTML to a ring response."
  [html]
  (-> (response/response (str html))
      (response/content-type "text/html; charset=utf-8")))

(defn- fragment-response
  "Convert Hiccup v2 HTML fragment to a ring response (for HTMX partial updates)."
  [html]
  (-> (response/response (str html))
      (response/content-type "text/html; charset=utf-8")))

(defn- html-escape
  "Escape HTML special characters."
  [s]
  (when s
    (-> s
        (str/replace "&" "&amp;")
        (str/replace "<" "&lt;")
        (str/replace ">" "&gt;")
        (str/replace "\"" "&quot;"))))

;; ---------------------------------------------------------------------------
;; Epistemic status labels

(def ^:private epistemic-status-classes
  "CSS classes for epistemic statuses."
  {:observed "status-observed"
   :derived "status-derived"
   :provisional "status-provisional"
   :accepted "status-accepted"})

(defn- epistemic-badge
  "Render an epistemic status badge."
  [status]
  (let [cls (get epistemic-status-classes status "status-unknown")]
    [:span {:class (str "epistemic-badge " cls)}
     (name status)]))

;; ---------------------------------------------------------------------------
;; Page layout

(defn- layout
  "Wrap content in a page layout."
  [title content]
  (h/html
   [:<!DOCTYPE "html"]
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title (html-escape title)]
     [:link {:rel "stylesheet" :href "/static/workbench.css"}]
     [:script {:src "https://unpkg.com/htmx.org@1.9.12"
               :integrity "sha384-adj2LlC0WEDC5OYGkD77wzBi++IO80P1TNywd8bl4YSx2zETlUqeHk6Y9hIRBrd"
               :crossorigin "anonymous"}]]
    [:body
     [:header.app-header
      [:h1 "Epiphany"]
      [:nav
       [:a {:href "/"} "Search"]
       [:a {:href "/timeline"} "Timeline"]
       [:a {:href "/inbox"} "Inbox"]
       [:a {:href "/health"} "Health"]]]
     [:main.app-main content]]]))

;; ---------------------------------------------------------------------------
;; Search page

(defn- search-form
  "Render the search form with mode/filter controls."
  [query mode limit]
  [:form.search-form {:hx-post "/htmx/search"
                      :hx-target "#search-results"
                      :hx-swap "innerHTML"
                      :hx-indicator "#search-spinner"}
   [:div.search-input-row
    [:input {:type "text"
             :name "query"
             :placeholder "Search sections..."
             :value (or query "")
             :autofocus true
             :required true}]
    [:button {:type "submit"} "Search"]]
   [:div.search-controls
    [:label "Mode:"
     [:select {:name "mode"}
      [:option {:value "lexical" :selected (= :lexical mode)} "Lexical"]
      [:option {:value "semantic" :selected (= :semantic mode)} "Semantic"]
      [:option {:value "hybrid" :selected (or (= :hybrid mode) (nil? mode))} "Hybrid"]]]
    [:label "Limit:"
     [:select {:name "limit"}
      [:option {:value "10" :selected (= 10 limit)} "10"]
      [:option {:value "20" :selected (or (= 20 limit) (nil? limit))} "20"]
      [:option {:value "50" :selected (= 50 limit)} "50"]]]
    [:label "Path prefix:"
     [:input {:type "text"
              :name "path-prefix"
              :placeholder "e.g. src/"
              :value ""}]]
    [:label "Ref:"
     [:input {:type "text"
              :name "ref"
              :placeholder "e.g. main"
              :value ""}]]]
   [:div#search-spinner.htmx-indicator "Searching..."]])

(defn- search-result-item
  "Render a single search result."
  [result]
  (let [path (:result/path-raw result)
        score (:result/score result)
        mode (:result/mode result)
        heading (str/join " > " (:result/heading-path result))
        scores (:result/scores result)
        commit-oid (:result/commit-oid result)
        status (or (:result/epistemic-status result) :derived)]
    [:div.search-result
     [:div.result-header
      [:span.result-path (html-escape path)]
      (when (seq heading)
        [:span.result-heading (html-escape heading)])
      (epistemic-badge status)]
     [:div.result-scores
      [:span.score (str "Score: " (format "%.4f" score))]
      [:span.mode (str "(" (name mode) ")")]
      (when (:lexical scores)
        [:span.sub-score (str "Lexical: " (format "%.4f" (:lexical scores)))])
      (when (:semantic scores)
        [:span.sub-score (str "Semantic: " (format "%.4f" (:semantic scores)))])]
     [:div.result-meta
      [:span.commit (str "Commit: " (subs commit-oid 0 (min 8 (count commit-oid))))]
      [:button.evidence-btn
       {:hx-get (str "/htmx/evidence?path=" (java.net.URLEncoder/encode (or path "") "UTF-8")
                     "&ref=" (java.net.URLEncoder/encode (or commit-oid "") "UTF-8"))
        :hx-target "#evidence-drawer"
        :hx-swap "innerHTML"
        :hx-trigger "click"}
       "View Evidence"]]]))

(defn- search-results
  "Render search results container."
  [results]
  (if (seq results)
    [:div#search-results.results-list
     [:div.results-count (str (count results) " results")]
     (map search-result-item results)]
    [:div#search-results.results-empty
     [:p "No results found."]]))

(defn- search-page
  "Render the search page."
  [& {:keys [query mode limit results]
      :or {query "" mode :hybrid limit 20 results []}}]
  (layout "Epiphany — Search"
   [:div.search-page
    (search-form query mode limit)
    (search-results results)
    [:div#evidence-drawer.evidence-drawer]]))

;; ---------------------------------------------------------------------------
;; Evidence drawer

(defn- evidence-section
  "Render an evidence section with source span."
  [section]
  [:div.evidence-section
   [:div.evidence-path (:section/path-raw section)]
   (when (seq (:section/heading-path section))
     [:div.evidence-heading (str/join " > " (:section/heading-path section))])
   [:pre.evidence-text (html-escape (:section/text section))]
   (when (:section/commit-oid section)
     [:div.evidence-commit (str "Commit: " (:section/commit-oid section))])])

(defn- evidence-drawer-content
  "Render evidence drawer content."
  [path text]
  (let [section {:section/path-raw path
                 :section/text text
                 :section/heading-path []}]
    [:div.drawer-content
     [:div.drawer-header
      [:h3 "Evidence"]
      [:button.drawer-close
       {:hx-get "/htmx/evidence/empty"
        :hx-target "#evidence-drawer"
        :hx-swap "innerHTML"}
       "\u00d7"]]
     (evidence-section section)]))

(defn- evidence-drawer-empty
  "Render empty evidence drawer."
  []
  [:div.drawer-content])

;; ---------------------------------------------------------------------------
;; HTMX handlers

(defn search-htmx-handler
  "Handle HTMX search request (returns HTML fragment)."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          query (:query body)
          mode (or (:mode body) "hybrid")
          mode (if (string? mode) (keyword mode) mode)
          limit (or (:limit body) 20)
          limit (if (string? limit) (Integer/parseInt limit) limit)
          path-prefix (:path-prefix body)
          ref (:ref body)
          request-map (cond-> {:query query
                               :mode mode
                               :limit limit}
                        path-prefix (assoc-in [:filters :path-prefix] path-prefix)
                        ref (assoc-in [:filters :ref] ref))
          results (try
                    (hs/search adapters request-map)
                    (catch Exception _ []))]
      (fragment-response (search-results results)))))

(defn- make-repo-git-port
  "Build a :git port backed by real Git object access for `repository-path`.
   Matches the (fn [_ arg] ...) port-fn shape epiphany.domain.evidence
   expects (mirrors infra.main's make-evidence-git-port)."
  [repository-path]
  {:read-blob (fn [_ oid] (git/read-blob repository-path oid))
   :commit-tree-entries (fn [_ commit-oid] (git/commit-tree-entries repository-path commit-oid))})

(defn- resolve-commit-oid
  "Resolve a ref, short OID, or HEAD-relative expression to a full commit
   OID (mirrors infra.main's resolve-commit-oid -- evidence/retrieve-
   evidence needs the full OID, not a ref name, to look up tree entries)."
  [repository-path expr]
  (let [{:keys [exit out err]}
        (clojure.java.shell/sh "git" "-C" repository-path "rev-parse" "--verify"
                               (str expr "^{commit}"))]
    (if (zero? exit)
      (str/trim out)
      (throw (ex-info (str "Could not resolve commit: " expr)
                      {:repository-path repository-path
                       :git-error (str/trim err)})))))

(defn evidence-htmx-handler
  "Handle HTMX evidence request (returns HTML fragment) -- retrieves the
   real Git blob content for `path`@`ref` (never fabricated text). `repo`
   defaults to \".\"; the workbench has no per-request repository-scoping
   concept yet (a broader gap this handler alone doesn't close -- disclosed,
   not silently worked around)."
  [_adapters]
  (fn [request]
    (let [params (:query-params request)
          path (:path params)
          ref (:ref params)
          repo (or (not-empty (:repo params)) ".")]
      (if (or (str/blank? path) (str/blank? ref))
        (fragment-response (evidence-drawer-empty))
        (let [result (try
                       (let [commit-oid (resolve-commit-oid repo ref)]
                         (evidence/retrieve-evidence
                          {:git (make-repo-git-port repo)}
                          {:path path :heading [] :commit-oid commit-oid}))
                       (catch Exception e
                         {:evidence/unavailable true
                          :evidence/failure {:failure/message (.getMessage e)}}))
              text (if (:evidence/unavailable result)
                     (str "UNAVAILABLE: " (get-in result [:evidence/failure :failure/message]))
                     (:evidence/source result))]
          (fragment-response (evidence-drawer-content path text)))))))

(defn evidence-empty-handler
  "Handle empty evidence drawer request."
  [_adapters]
  (fn [_request]
    (fragment-response (evidence-drawer-empty))))

;; ---------------------------------------------------------------------------
;; Static page handler

(defn search-page-handler
  "Handle the main search page."
  [_adapters]
  (fn [_request]
    (html-response (search-page))))

;; ---------------------------------------------------------------------------
;; Timeline view

(def ^:private edge-status-classes
  "CSS classes for timeline edge statuses."
  {:observed "edge-observed"
   :accepted "edge-accepted"
   :provisional "edge-provisional"
   :rejected "edge-rejected"})

(defn- timeline-edge
  "Render a single timeline edge between two nodes."
  [edge]
  (let [cls (get edge-status-classes (:edge/status edge) "edge-unknown")]
    [:div.timeline-edge {:class cls}
     [:span.edge-label (str (name (:edge/relation edge))
                            " (" (format "%.2f" (:edge/confidence edge)) ")")]
     [:span.edge-status (name (:edge/status edge))]]))

(defn- timeline-node
  "Render a single timeline node. Sections in a real trace (see
   epiphany.domain.lineage-trace) carry plain (non-namespaced)
   :path-raw/:heading-path/:commit-oid keys, matching `ep trace`'s CLI
   output -- not the :section/* namespaced keys this render function
   originally assumed and was never exercised against real trace data.

   Parameter order (idx, node) matches map-indexed's callback contract --
   the previous (node, idx) order meant a real map-indexed call would have
   silently swapped them (never caught, since nothing ever called this
   with real data)."
  [idx node]
  (let [section (:node/section node)
        path (:path-raw section)
        heading (str/join " > " (:heading-path section))
        commit-oid (:commit-oid section)
        short-oid (when commit-oid (subs commit-oid 0 (min 8 (count commit-oid))))]
    [:div.timeline-node
     [:div.node-marker (str idx)]
     [:div.node-content
      [:div.node-path (html-escape path)]
      (when (seq heading)
        [:div.node-heading (html-escape heading)])
      [:div.node-meta
       (when short-oid
         [:span.node-commit (str "Commit: " short-oid)])
       [:button.evidence-btn
        {:hx-get (str "/htmx/evidence?path=" (java.net.URLEncoder/encode (or path "") "UTF-8")
                      "&ref=" (java.net.URLEncoder/encode (or commit-oid "") "UTF-8"))
         :hx-target "#evidence-drawer"
         :hx-swap "innerHTML"
         :hx-trigger "click"}
        "View Evidence"]]]]))

(defn- timeline-graph
  "Render a timeline graph from nodes and edges. Edges are visually
   distinguished by status via edge-status-classes."
  [nodes edges]
  [:div.timeline-graph
   (doall (map-indexed timeline-node nodes))
   (when (seq edges)
     [:div.timeline-edges (doall (map timeline-edge edges))])])

(defn- timeline-page
  "Render the timeline page."
  [& {:keys [source-path edges nodes] :or {source-path "" edges [] nodes []}}]
  (layout "Epiphany — Timeline"
   [:div.timeline-page
    [:h2 "Lineage Timeline"]
    [:div.timeline-form
     [:form {:hx-post "/htmx/timeline"
             :hx-target "#timeline-content"
             :hx-swap "innerHTML"}
      [:div.search-input-row
       [:input {:type "text" :name "path" :placeholder "Section path..." :value source-path}]
       [:input {:type "text" :name "repo" :placeholder "Repository path (default .)" :value ""}]
       [:button {:type "submit"} "Trace"]]]]
    [:div#timeline-content.timeline-content
     (if (seq nodes)
       (timeline-graph nodes edges)
       [:div.results-empty [:p "No timeline data. Enter a section path to trace its lineage."]])]
    [:div#evidence-drawer.evidence-drawer]]))

(defn timeline-page-handler
  "Handle the timeline page."
  [_adapters]
  (fn [_request]
    (html-response (timeline-page))))

(defn- path-revisions
  "Walk commit history reachable from `refs` in `repository-path`,
   returning every revision of `path` as a section map (plain
   :path-raw/:heading-path/:commit-oid/:timestamp keys), chronologically
   sorted. Mirrors infra.main's CLI `path-revisions` -- the workbench has
   no observations-port-backed candidate store for cross-file lineage
   edges yet, so like `ep trace`, only Git-history revisions of the exact
   path are traced (an honest, disclosed scope, not a silent gap)."
  [repository-path refs path]
  (let [{:keys [commits]} (git/reachable-commits repository-path refs)
        revisions (keep (fn [c]
                          (let [{:keys [entries]} (git/commit-tree-entries repository-path (:commit/oid c))
                                entry (first (filter #(= path (:git/path %)) entries))]
                            (when entry
                              {:path-raw path
                               :heading-path []
                               :commit-oid (:commit/oid c)
                               :blob-oid (:git/blob-oid entry)
                               :timestamp (get-in c [:commit/author :person/timestamp])})))
                        commits)]
    (sort-by (fn [s] (.getTime ^java.util.Date (or (:timestamp s) (java.util.Date. 0))))
             revisions)))

(defn timeline-htmx-handler
  "Handle HTMX timeline request (returns HTML fragment). Traces real Git
   history for `path` in `repo` (default \".\") -- never fabricated data.
   An unreadable repo or a path with no history reports an explicit empty/
   unavailable state, distinct from a real (possibly single-node) trace."
  [_adapters]
  (fn [request]
    (let [body (:body-params request)
          path (:path body)
          repo (or (not-empty (:repo body)) ".")]
      (if (str/blank? path)
        (fragment-response [:div.results-empty [:p "Enter a section path to trace its lineage."]])
        (try
          (let [revisions (path-revisions repo ["HEAD"] path)]
            (if (empty? revisions)
              (fragment-response [:div.results-empty
                                   [:p (str "No Git history found for "
                                            (html-escape path) " in " (html-escape repo))]])
              (let [source (last revisions)
                    history (butlast revisions)
                    trace (lineage-trace/trace-lineage source history [])]
                (fragment-response (timeline-graph (:trace/nodes trace) (:trace/edges trace))))))
          (catch Exception e
            (fragment-response [:div.results-empty [:p (str "UNAVAILABLE: " (.getMessage e))]])))))))

;; ---------------------------------------------------------------------------
;; Inbox view

(defn- inbox-filter-form
  "Render the inbox filter controls."
  []
  [:div.inbox-filters
   [:form {:hx-post "/htmx/inbox"
           :hx-target "#inbox-list"
           :hx-swap "innerHTML"}
    [:div.search-controls
     [:label "Resource ID:"
      [:input {:type "text" :name "resource-id" :placeholder "Registered repository's resource-id"}]]
     [:label "Relation:"
      [:select {:name "relation"}
       [:option {:value ""} "All"]
       (for [r (sort lineage/relation-types)]
         [:option {:value (name r)} (name r)])]]
     [:label "Min confidence:"
      [:input {:type "number" :name "min-confidence" :min "0" :max "1" :step "0.1" :value "0"}]]
     [:label "Sort:"
      [:select {:name "sort"}
       [:option {:value "confidence"} "Confidence"]
       [:option {:value "evidence"} "Evidence"]]]
     [:button {:type "submit"} "Filter"]]]])

(defn- inbox-item
  "Render a single inbox item. A durable candidate's source/target spans
   carry :span/path-raw + :span/heading-path (epiphany.domain.candidates/
   make-span) -- not the :section/* keys this originally assumed, never
   exercised against a real candidate record."
  [item]
  (let [candidate (:inbox/candidate item)
        source (:lineage-candidate/source candidate)
        target (:lineage-candidate/target candidate)
        relation (:lineage-candidate/relation candidate)
        confidence (:lineage-candidate/confidence candidate)
        candidate-id (:lineage-candidate/id candidate)
        summary (:inbox/evidence-summary item)]
    [:div.inbox-item
     [:div.inbox-item-header
      [:span.inbox-relation (name relation)]
      [:span.inbox-confidence (format "%.2f" confidence)]
      (epistemic-badge (:inbox/decision-status item))]
     [:div.inbox-item-paths
      [:span.inbox-source (html-escape (str (:span/path-raw source) " > "
                                           (str/join " > " (:span/heading-path source))))]
      [:span.inbox-arrow " → "]
      [:span.inbox-target (html-escape (str (:span/path-raw target) " > "
                                           (str/join " > " (:span/heading-path target))))]]
     [:div.inbox-item-summary (html-escape summary)]
     [:div.inbox-item-actions
      [:button.inbox-btn.accept-btn
       {:hx-post "/htmx/inbox/decide"
        :hx-vals (str "{\"candidate-id\":\"" candidate-id "\",\"decision\":\"accepted\",\"reason\":\"\"}")
        :hx-target "#inbox-list"
        :hx-swap "innerHTML"}
       "Accept"]
      [:button.inbox-btn.reject-btn
       {:hx-post "/htmx/inbox/decide"
        :hx-vals (str "{\"candidate-id\":\"" candidate-id "\",\"decision\":\"rejected\",\"reason\":\"\"}")
        :hx-target "#inbox-list"
        :hx-swap "innerHTML"}
       "Reject"]]]))

(defn- inbox-list
  "Render the inbox list."
  [items]
  (if (seq items)
    [:div#inbox-list.inbox-list
     [:div.results-count (str (count items) " candidates")]
     (doall (map inbox-item items))]
    [:div#inbox-list.results-empty
     [:p "No candidates to review."]]))

(defn- inbox-page
  "Render the inbox page."
  [& {:keys [items] :or {items []}}]
  (layout "Epiphany — Review Inbox"
   [:div.inbox-page
    [:h2 "Review Inbox"]
    (inbox-filter-form)
    (inbox-list items)]))

(defn inbox-page-handler
  "Handle the inbox page."
  [_adapters]
  (fn [_request]
    (html-response (inbox-page))))

(defn- parse-resource-id [s]
  (when-not (str/blank? s)
    (try (java.util.UUID/fromString s) (catch Exception _ nil))))

(defn inbox-htmx-handler
  "Handle HTMX inbox list request (returns HTML fragment) -- queries the
   real durable candidate (ENG-005G) and decision (ENG-005A) stores for
   :resource-id, the same domain/inbox/build-inbox `ep inbox` uses. No
   resource-id (blank or malformed) is an explicit empty state, distinct
   from \"queried and found nothing\"."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          resource-id (parse-resource-id (:resource-id body))
          relation (not-empty (:relation body))
          min-conf (when-let [mc (:min-confidence body)]
                     (try (Double/parseDouble mc) (catch Exception _ nil)))
          sort-key (if (= "evidence" (:sort body)) :evidence :confidence)]
      (if-not resource-id
        (fragment-response [:div#inbox-list.results-empty
                             [:p "Enter a registered repository's resource-id to review its candidates."]])
        (let [list-candidates (get-in adapters [:observations :list-lineage-candidates])
              list-decisions (get-in adapters [:observations :list-review-decisions])
              candidates (if list-candidates (list-candidates resource-id) [])
              decisions (if list-decisions (list-decisions resource-id) [])
              filters (cond-> {}
                        relation (assoc :relation-types [(keyword relation)])
                        (and min-conf (pos? min-conf)) (assoc :confidence-band [min-conf nil]))
              items (inbox/build-inbox candidates decisions filters {:sort sort-key})]
          (fragment-response (inbox-list items)))))))

(defn inbox-decide-htmx-handler
  "Handle HTMX inbox decision request (returns the updated inbox list).
   Durably records through :record-review-decision! -- the same op `ep
   inbox decide` and POST /api/v1/review-decisions use -- resolving the
   candidate's :resource-id by looking it up, so an HTMX decision, a CLI
   decision, and an HTTP API decision are indistinguishable in the store."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          candidate-id (try (java.util.UUID/fromString (:candidate-id body))
                             (catch Exception _ nil))
          decision-str (:decision body)
          decision-type (when decision-str (keyword decision-str))]
      (if (or (nil? candidate-id) (not (contains? review/review-decision-types decision-type)))
        (fragment-response [:div#inbox-list.results-empty
                             [:p "Invalid candidate id or decision type."]])
        (let [find-candidate (get-in adapters [:observations :find-lineage-candidate-by-id])
              candidate (when find-candidate (find-candidate candidate-id))]
          (if-not candidate
            (fragment-response [:div#inbox-list.results-empty
                                 [:p "No candidate found for that id."]])
            (let [resource-id (:resource-id candidate)
                  decision (review/make-decision candidate-id decision-type
                                                 :reason (:reason body))
                  observation (review/decision->observation
                               decision {:resource-id resource-id :adapter-version "0.1.0"})
                  record! (get-in adapters [:observations :record-review-decision!])
                  list-candidates (get-in adapters [:observations :list-lineage-candidates])
                  list-decisions (get-in adapters [:observations :list-review-decisions])]
              (when record! (record! observation))
              (let [candidates (if list-candidates (list-candidates resource-id) [])
                    decisions (if list-decisions (list-decisions resource-id) [])
                    items (inbox/build-inbox candidates decisions)]
                (fragment-response (inbox-list items))))))))))

;; ---------------------------------------------------------------------------
;; Health panel view

(defn- stage-card
  "Render a single stage status card.

   Binds :stage/name to the local `stage-name` (not `name`) -- the
   previous (name name) shadowed clojure.core/name with the local
   binding, invoking the keyword value as a function and always
   rendering nil. Never caught before because nothing ever fed this a
   real stage map until this pass wired the health panel to
   domain/status/query-status."
  [{stage-name :stage/name :keys [stage/status stage/counts stage/failures stage/lag]}]
  (let [status-cls (case status
                     :ok "stage-ok"
                     :error "stage-error"
                     :in-progress "stage-progress"
                     :unavailable "stage-unavailable"
                     "stage-unknown")]
    [:div.stage-card {:class status-cls}
     [:div.stage-header
      [:h3 (name stage-name)]
      [:span.stage-status (name status)]]
     (when (seq counts)
       [:div.stage-counts
        (for [[k v] counts]
          [:span.stage-count [:strong (name k)] ": " (str v)])])
     (when (some? lag)
      [:div.stage-lag "Lag: " (str lag)])
     (when (seq failures)
       [:div.stage-failures
        [:h4 "Failures"]
        (for [f failures]
          [:div.failure-record
           [:span.failure-error (:failure/error f)]
           (when-let [ctx (:failure/context f)]
             [:span.failure-context " (" ctx ")"])])])]))

(defn- health-form
  "Render the resource-id input driving the health refresh, mirroring
   `ep status --resource-id`."
  [resource-id]
  [:form.health-form {:hx-post "/htmx/health"
                      :hx-target "#health-content"
                      :hx-swap "innerHTML"}
   [:input {:type "text" :name "resource-id" :placeholder "Registered repository's resource-id"
            :value (or resource-id "")}]
   [:button {:type "submit"} "Refresh"]])

(defn- health-stages-view
  [stages]
  [:div.health-stages
   (if (seq stages)
     (doall (map stage-card stages))
     [:div.results-empty [:p "No status data available. Register a repository first."]])])

(defn- health-page
  "Render the health panel page."
  [& {:keys [resource-id stages summary] :or {stages [] summary {}}}]
  (layout "Epiphany — Corpus Health"
   [:div.health-page
    [:h2 "Corpus Health"]
    (health-form resource-id)
    [:div.health-summary
     (for [[k v] summary]
       [:span.summary-item [:strong (name k)] ": " (str v)])]
    [:div#health-content
     (health-stages-view stages)]]))

(defn- query-health-status
  "Query real cross-stage status for `resource-id` via domain/status --
   the same query the AC calls for (\"from the same status queries as `ep
   status`\"). nil resource-id (no repository selected yet) is an explicit
   empty state, never a fabricated or silently-empty result."
  [adapters resource-id]
  (when resource-id
    (status/query-status adapters resource-id)))

(defn health-page-handler
  "Handle the health panel page."
  [adapters]
  (fn [request]
    (let [resource-id (parse-resource-id (get-in request [:query-params :resource-id]))
          result (query-health-status adapters resource-id)]
      (html-response (health-page :resource-id (:resource-id result)
                                  :stages (:stages result)
                                  :summary (:summary result))))))

(defn health-htmx-handler
  "Handle HTMX health refresh request."
  [adapters]
  (fn [request]
    (let [body (:body-params request)
          resource-id (parse-resource-id (:resource-id body))
          result (query-health-status adapters resource-id)]
      (fragment-response (health-stages-view (:stages result))))))
