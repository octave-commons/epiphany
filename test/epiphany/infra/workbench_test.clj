(ns epiphany.infra.workbench-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.workbench :as wb]
            [epiphany.infra.http :as http]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.domain.candidates :as candidates]
            [epiphany.domain.review :as review]))

;; ---------------------------------------------------------------------------
;; Mock adapters

(defn- mock-adapters []
  {:git {:repository (constantly ::mock-repo)
         :resolve-ref (constantly ["refs/heads/main"])}
   :repository-metadata {:list-repositories (constantly [])}
   :observations {:list-checkpoints (constantly [])
                  :list-ingestion-runs (constantly [])}
   :index {:search (constantly [])
           :knn-search (constantly [])
           :index-stats (constantly {:document-count 0})}
   :embeddings {:embed-query (constantly [0.1 0.2 0.3])}})

;; ---------------------------------------------------------------------------
;; Real in-memory adapters, for genuine round-trip tests over the inbox and
;; health panel (candidates/decisions/status queries, not hand-rolled mocks).

(defn- real-adapters []
  (in-memory/make {:common-git-dir-fn (fn [path] (str path "/.git"))}))

(defn- seed-candidate! [obs resource-id relation]
  (let [candidate (candidates/make-candidate
                   relation
                   (candidates/make-span {:path-raw "a.md" :heading-path ["Intro"]
                                          :commit-oid "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
                   (candidates/make-span {:path-raw "b.md" :heading-path ["Intro"]
                                          :commit-oid "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"})
                   :confidence 0.9
                   :generator-version "test-v1")
        observation (candidates/candidate->observation
                     candidate {:resource-id resource-id :adapter-version "0.1.0"})]
    ((:record-lineage-candidate! obs) observation)
    (:lineage-candidate/id candidate)))

;; ---------------------------------------------------------------------------
;; search-page-handler tests

(deftest search-page-handler-returns-html
  (testing "Search page returns HTML with search form"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/" :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "text/html"))
      (is (.contains (:body resp) "Epiphany"))
      (is (.contains (:body resp) "search-form"))
      (is (.contains (:body resp) "htmx.org")))))

(deftest search-page-handler-has-mode-controls
  (testing "Search page has mode selection controls"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/" :headers {}})]
      (is (.contains (:body resp) "lexical"))
      (is (.contains (:body resp) "semantic"))
      (is (.contains (:body resp) "hybrid")))))

(deftest search-page-handler-has-filter-controls
  (testing "Search page has path prefix and ref filters"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/" :headers {}})]
      (is (.contains (:body resp) "path-prefix"))
      (is (.contains (:body resp) "ref")))))

;; ---------------------------------------------------------------------------
;; search-htmx-handler tests

(deftest search-htmx-handler-returns-fragment
  (testing "HTMX search returns HTML fragment"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/search"
                     :body-params {:query "test" :mode "hybrid" :limit 10}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "text/html"))
      (is (.contains (:body resp) "search-results")))))

(deftest search-htmx-handler-with-empty-query
  (testing "HTMX search with empty query returns results container"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/search"
                     :body-params {:query "" :mode "hybrid" :limit 10}
                     :headers {}})]
      (is (= 200 (:status resp))))))

;; ---------------------------------------------------------------------------
;; evidence-htmx-handler tests

(deftest evidence-htmx-handler-returns-drawer
  (testing "Evidence handler returns drawer content"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/htmx/evidence?path=doc.md&ref=abc123"
                     :query-params {"path" "doc.md" "ref" "abc123"}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "drawer-content"))
      (is (.contains (:body resp) "doc.md")))))

(deftest evidence-htmx-handler-shows-evidence-text
  (testing "Evidence handler retrieves real Git blob content, never fabricated text"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/htmx/evidence?path=AGENTS.md&ref=HEAD"
                     :query-params {}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "Epiphany")))))

(deftest evidence-htmx-handler-requires-a-ref
  (testing "A path with no ref is an empty drawer, not fabricated text"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/htmx/evidence?path=test.md"
                     :query-params {}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (not (.contains (:body resp) "Evidence for"))))))

(deftest evidence-htmx-handler-reports-unavailable-for-missing-path
  (testing "A nonexistent path reports UNAVAILABLE, never invented content"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/htmx/evidence?path=no/such/path.md&ref=HEAD"
                     :query-params {}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "UNAVAILABLE")))))

;; ---------------------------------------------------------------------------
;; evidence-empty-handler tests

(deftest evidence-empty-handler-returns-empty-drawer
  (testing "Empty evidence handler returns empty drawer"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/htmx/evidence/empty"
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "drawer-content")))))

;; ---------------------------------------------------------------------------
;; Static file serving tests

(deftest static-css-served
  (testing "Static CSS file is served"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/static/workbench.css" :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "text/css")))))

;; ---------------------------------------------------------------------------
;; Integration with API routes

(deftest api-search-still-works
  (testing "API search route still works alongside workbench"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/search"
                     :body-params {:query "test" :mode :hybrid :limit 10}
                     :headers {"accept" "application/json"}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/json")))))

;; ---------------------------------------------------------------------------
;; HTML content validation

(deftest search-page-has-htmx-attributes
  (testing "Search form has HTMX attributes"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/" :headers {}})]
      (is (.contains (:body resp) "hx-post"))
      (is (.contains (:body resp) "hx-target"))
      (is (.contains (:body resp) "hx-swap")))))

(deftest search-page-has-evidence-drawer
  (testing "Search page has evidence drawer div"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/" :headers {}})]
      (is (.contains (:body resp) "evidence-drawer")))))

(deftest css-file-exists
  (testing "workbench.css resource file exists"
    (is (.exists (clojure.java.io/file "resources/public/workbench.css")))))

;; ---------------------------------------------------------------------------
;; timeline-page-handler tests

(deftest timeline-page-handler-returns-html
  (testing "Timeline page returns HTML with timeline form"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/timeline" :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "Epiphany"))
      (is (.contains (:body resp) "Lineage Timeline"))
      (is (.contains (:body resp) "timeline-form"))
      (is (.contains (:body resp) "htmx.org")))))

(deftest timeline-page-has-nav-links
  (testing "Timeline page has navigation links"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/timeline" :headers {}})]
      (is (.contains (:body resp) "Search"))
      (is (.contains (:body resp) "Timeline"))
      (is (.contains (:body resp) "Inbox"))
      (is (.contains (:body resp) "Health")))))

(deftest timeline-htmx-handler-returns-fragment
  (testing "HTMX timeline traces real Git history for a tracked path in this repo"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/timeline"
                     :body-params {:path "AGENTS.md"}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "timeline-graph"))
      (is (.contains (:body resp) "AGENTS.md"))
      (is (.contains (:body resp) "edge-observed")))))

(deftest timeline-htmx-handler-reports-no-history-for-untracked-path
  (testing "An untracked path reports no history explicitly, never a fabricated node"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/timeline"
                     :body-params {:path "no/such/path.md"}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "No Git history found")))))

(deftest timeline-htmx-handler-empty-path
  (testing "HTMX timeline with empty path shows placeholder"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/timeline"
                     :body-params {:path ""}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "Enter a section path")))))

;; ---------------------------------------------------------------------------
;; inbox-page-handler tests

(deftest inbox-page-handler-returns-html
  (testing "Inbox page returns HTML with inbox list"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/inbox" :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "Review Inbox"))
      (is (.contains (:body resp) "inbox-list")))))

(deftest inbox-page-has-filter-form
  (testing "Inbox page has filter controls"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/inbox" :headers {}})]
      (is (.contains (:body resp) "inbox-filters"))
      (is (.contains (:body resp) "min-confidence")))))

(deftest inbox-htmx-handler-returns-fragment
  (testing "HTMX inbox returns HTML fragment"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/inbox"
                     :body-params {:relation "" :min-confidence "0" :sort "confidence"}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "inbox-list")))))

(deftest inbox-decide-htmx-handler-returns-fragment
  (testing "HTMX inbox decision with an invalid candidate id/decision reports an explicit error, not a crash"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/inbox/decide"
                     :body-params {:candidate-id "test-id" :decision "accepted" :reason ""}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "inbox-list")))))

(deftest inbox-htmx-handler-real-round-trip
  (testing "the inbox lists a real durable candidate and honors suppression, via HTTP"
    (let [adapters (real-adapters)
          obs (:observations adapters)
          resource-id (random-uuid)
          _ (seed-candidate! obs resource-id :continues)
          app (http/make-router adapters)
          resp (app {:request-method :post
                     :uri "/htmx/inbox"
                     :body-params {:resource-id (str resource-id) :relation "" :min-confidence "0" :sort "confidence"}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "1 candidates"))
      (is (.contains (:body resp) "continues")))))

(deftest inbox-decide-htmx-handler-real-round-trip
  (testing "a real decision durably suppresses the candidate from the inbox afterward"
    (let [adapters (real-adapters)
          obs (:observations adapters)
          resource-id (random-uuid)
          candidate-id (seed-candidate! obs resource-id :continues)
          app (http/make-router adapters)
          decide-resp (app {:request-method :post
                            :uri "/htmx/inbox/decide"
                            :body-params {:candidate-id (str candidate-id) :decision "rejected" :reason ""}
                            :headers {}})
          list-resp (app {:request-method :post
                          :uri "/htmx/inbox"
                          :body-params {:resource-id (str resource-id)}
                          :headers {}})]
      (is (= 200 (:status decide-resp)))
      (is (.contains (:body decide-resp) "No candidates to review."))
      (is (.contains (:body list-resp) "No candidates to review."))
      (let [decisions ((:list-review-decisions obs) resource-id)]
        (is (= 1 (count decisions)))
        (is (not (contains? (first decisions) :review-decision/reason)))))))

;; ---------------------------------------------------------------------------
;; health-page-handler tests

(deftest health-page-handler-returns-html
  (testing "Health page returns HTML with health stages"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/health" :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "Corpus Health"))
      (is (.contains (:body resp) "health-stages")))))

(deftest health-page-has-summary
  (testing "Health page has summary section"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/health" :headers {}})]
      (is (.contains (:body resp) "health-summary")))))

(deftest health-htmx-handler-returns-fragment
  (testing "HTMX health refresh with no resource-id is an explicit empty state"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/htmx/health"
                     :body-params {}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "health-stages"))
      (is (.contains (:body resp) "Register a repository first")))))

(deftest health-htmx-handler-real-round-trip
  (testing "the health panel queries real registration/extraction status for a resource-id"
    (let [adapters (real-adapters)
          resource-id (random-uuid)
          _ ((get-in adapters [:repository-metadata :write]) "/repo/.git" resource-id)
          app (http/make-router adapters)
          resp (app {:request-method :post
                     :uri "/htmx/health"
                     :body-params {:resource-id (str resource-id)}
                     :headers {}})]
      (is (= 200 (:status resp)))
      (is (.contains (:body resp) "registration"))
      (is (.contains (:body resp) "registered")))))
