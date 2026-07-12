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
            [epiphany.domain.review :as review]
            [epiphany.domain.status :as status]))

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

(defn evidence-htmx-handler
  "Handle HTMX evidence request (returns HTML fragment)."
  [_adapters]
  (fn [request]
    (let [params (:query-params request)
          path (:path params)
          text (str "Evidence for: " path)]
      (fragment-response (evidence-drawer-content path text)))))

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
  "Render a single timeline node."
  [node idx]
  (let [section (:node/section node)
        path (:section/path-raw section)
        heading (str/join " > " (:section/heading-path section))
        commit-oid (:section/commit-oid section)
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
       [:button {:type "submit"} "Trace"]]]]
    [:div#timeline-content.timeline-content
     (if (seq nodes)
       [:div.timeline-graph
        (interleave
         (map-indexed timeline-node nodes)
         (map timeline-edge edges))]
       [:div.results-empty [:p "No timeline data. Enter a section path to trace its lineage."]])]
    [:div#evidence-drawer.evidence-drawer]]))

(defn- timeline-graph
  "Render a timeline graph from nodes and edges."
  [nodes edges]
  [:div.timeline-graph
   (doall
    (interleave
     (map-indexed timeline-node nodes)
     (map timeline-edge edges)))])

(defn timeline-page-handler
  "Handle the timeline page."
  [_adapters]
  (fn [_request]
    (html-response (timeline-page))))

(defn timeline-htmx-handler
  "Handle HTMX timeline request (returns HTML fragment)."
  [_adapters]
  (fn [request]
    (let [body (:body-params request)
          path (:path body)]
      (if (str/blank? path)
        (fragment-response [:div.results-empty [:p "Enter a section path to trace its lineage."]])
        ;; With real data, we'd query lineage here. For now return placeholder.
        (fragment-response
         [:div.timeline-graph
          [:div.timeline-node
           [:div.node-marker "0"]
           [:div.node-content
            [:div.node-path (html-escape path)]
            [:div.node-meta
             [:span.node-commit "No lineage data yet"]]]]])))))

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
  "Render a single inbox item."
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
      (epistemic-badge (or (:lineage-candidate/status candidate) :provisional))]
     [:div.inbox-item-paths
      [:span.inbox-source (html-escape (str (:section/path-raw source) " > "
                                           (str/join " > " (:section/heading-path source))))]
      [:span.inbox-arrow " → "]
      [:span.inbox-target (html-escape (str (:section/path-raw target) " > "
                                           (str/join " > " (:section/heading-path target))))]]
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

(defn inbox-htmx-handler
  "Handle HTMX inbox list request (returns HTML fragment)."
  [_adapters]
  (fn [request]
    (let [body (:body-params request)
          relation (:relation body)
          min-conf (when-let [mc (:min-confidence body)]
                     (try (Double/parseDouble mc) (catch Exception _ 0.0)))
          sort-by (or (:sort body) "confidence")
          ;; With real data we'd query candidates/decisions. For now return placeholder.
          items []]
      (fragment-response (inbox-list items)))))

(defn inbox-decide-htmx-handler
  "Handle HTMX inbox decision request (returns updated inbox list)."
  [_adapters]
  (fn [request]
    (let [body (:body-params request)
          candidate-id (:candidate-id body)
          decision (:decision body)]
      ;; With real data we'd record the decision via review/make-decision.
      ;; For now return the (empty) inbox list.
      (fragment-response (inbox-list [])))))

;; ---------------------------------------------------------------------------
;; Health panel view

(defn- stage-card
  "Render a single stage status card."
  [{:keys [stage/name stage/status stage/counts stage/failures stage/lag]}]
  (let [status-cls (case status
                     :ok "stage-ok"
                     :error "stage-error"
                     :in-progress "stage-progress"
                     :unavailable "stage-unavailable"
                     "stage-unknown")]
    [:div.stage-card {:class status-cls}
     [:div.stage-header
      [:h3 (name name)]
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

(defn- health-page
  "Render the health panel page."
  [& {:keys [stages summary] :or {stages [] summary {}}}]
  (layout "Epiphany — Corpus Health"
   [:div.health-page
    [:h2 "Corpus Health"]
    [:div.health-summary
     (for [[k v] summary]
       [:span.summary-item [:strong (name k)] ": " (str v)])]
    [:div.health-stages
     (if (seq stages)
       (doall (map stage-card stages))
       [:div.results-empty [:p "No status data available. Register a repository first."]])]]))

(defn health-page-handler
  "Handle the health panel page."
  [adapters]
  (fn [_request]
    (let [;; With real data we'd query all repositories and aggregate status.
          stages []
          summary {}]
      (html-response (health-page :stages stages :summary summary)))))

(defn health-htmx-handler
  "Handle HTMX health refresh request."
  [adapters]
  (fn [_request]
    (let [stages []
          summary {}]
      (fragment-response
       [:div.health-stages
        (if (seq stages)
          (doall (map stage-card stages))
          [:div.results-empty [:p "No status data available."]])]))))
