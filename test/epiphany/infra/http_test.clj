(ns epiphany.infra.http-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [epiphany.infra.http :as http]))

;; ---------------------------------------------------------------------------
;; Mock adapters

(defn- mock-search-adapters []
  {:git {:repository (constantly ::mock-repo)
         :resolve-ref (constantly ["refs/heads/main"])}
   :repository-metadata {:list-repositories (constantly [])}
   :observations {:list-checkpoints (constantly [])
                  :list-ingestion-runs (constantly [])}
   :index {:search (constantly [])
           :knn-search (constantly [])
           :index-stats (constantly {:document-count 0})}
   :embeddings {:embed-query (constantly [0.1 0.2 0.3])}})

(defn- mock-adapters []
  {:git {:repository (constantly ::mock-repo)
         :resolve-ref (constantly ["refs/heads/main"])}
   :repository-metadata {:list-repositories (constantly [])
                         :register-repository (constantly {:id #uuid "00000000-0000-0000-0000-000000000001"})}
   :observations {:list-checkpoints (constantly [])
                  :list-ingestion-runs (constantly [])}
   :index {:search (constantly [])
           :index-stats (constantly {:document-count 0})}
   :embeddings {:embed (constantly [0.1 0.2 0.3])}})

;; ---------------------------------------------------------------------------
;; problem-response tests

(deftest problem-response-creates-rfc9457
  (testing "problem-response creates RFC 9457 response"
    (let [resp (http/problem-response 400 "Bad Request" "test detail")]
      (is (= 400 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/problem+json"))
      (let [body (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Bad Request" (:title body)))
        (is (= "test detail" (:detail body)))
        (is (= 400 (:status body)))))))

(deftest problem-response-with-errors
  (testing "problem-response includes errors"
    (let [resp (http/problem-response 422 "Unprocessable" "invalid" :errors ["err1" "err2"])]
      (let [body (json/read-str (:body resp) :key-fn keyword)]
        (is (= ["err1" "err2"] (:errors body)))))))

(deftest unavailable-problem-returns-503
  (testing "unavailable-problem returns 503"
    (let [resp (http/unavailable-problem "service down")]
      (is (= 503 (:status resp))))))

(deftest bad-request-problem-returns-400
  (testing "bad-request-problem returns 400"
    (let [resp (http/bad-request-problem "missing field")]
      (is (= 400 (:status resp))))))

(deftest not-found-problem-returns-404
  (testing "not-found-problem returns 404"
    (let [resp (http/not-found-problem "not found")]
      (is (= 404 (:status resp))))))

(deftest internal-error-problem-returns-500
  (testing "internal-error-problem returns 500"
    (let [resp (http/internal-error-problem "oops")]
      (is (= 500 (:status resp))))))

;; ---------------------------------------------------------------------------
;; Router tests

(deftest router-handles-not-found
  (testing "Router returns 404 for unknown routes"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get :uri "/api/v1/unknown"})]
      (is (= 404 (:status resp))))))

(deftest router-handles-search-post
  (testing "Router handles POST /api/v1/search"
    (let [app (http/make-router (mock-search-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/search"
                     :body-params {:query "test" :mode :hybrid :limit 10}
                     :headers {"accept" "application/json"}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/json")))))

(deftest router-handles-search-missing-query
  (testing "Router returns 400 for missing query"
    (let [app (http/make-router (mock-search-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/search"
                     :body-params {:query "" :mode :hybrid :limit 10}
                     :headers {}})]
      (is (= 400 (:status resp))))))

(deftest router-handles-search-invalid-mode
  (testing "Router returns 400 for invalid mode"
    (let [app (http/make-router (mock-search-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/search"
                     :body-params {:query "test" :mode :invalid :limit 10}
                     :headers {}})]
      (is (= 400 (:status resp))))))

(deftest router-handles-register-post
  (testing "Router handles POST /api/v1/register"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/register"
                     :body-params {:path "/tmp/test-repo"}
                     :headers {}})]
      (is (= 201 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/json")))))

(deftest router-handles-register-missing-path
  (testing "Router returns 400 for missing path"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/register"
                     :body-params {:path ""}
                     :headers {}})]
      (is (= 400 (:status resp))))))

(deftest router-handles-status-get
  (testing "Router handles GET /api/v1/status/:resource-id"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/api/v1/status/00000000-0000-0000-0000-000000000001"
                     :path-params {:resource-id "00000000-0000-0000-0000-000000000001"}
                     :headers {}})]
      (is (= 200 (:status resp))))))

(deftest router-handles-status-invalid-uuid
  (testing "Router returns 400 for invalid UUID"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/api/v1/status/not-a-uuid"
                     :path-params {:resource-id "not-a-uuid"}
                     :headers {}})]
      (is (= 400 (:status resp))))))

(deftest router-handles-review-decisions-post
  (testing "Router handles POST /api/v1/review-decisions"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/review-decisions"
                     :body-params {:decision "accepted"
                                   :candidate-id "cand-1"
                                   :rationale "good"}
                     :headers {}})]
      (is (= 201 (:status resp))))))

(deftest router-handles-review-decisions-missing-fields
  (testing "Router returns 400 for missing decision"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/review-decisions"
                     :body-params {:decision "" :candidate-id "c1" :rationale "r"}
                     :headers {}})]
      (is (= 400 (:status resp))))))

;; ---------------------------------------------------------------------------
;; Content negotiation tests

(deftest search-accepts-edn-response
  (testing "Search accepts EDN response format"
    (let [app (http/make-router (mock-search-adapters))
          resp (app {:request-method :post
                     :uri "/api/v1/search"
                     :body-params {:query "test" :mode :hybrid :limit 10}
                     :headers {"accept" "application/edn"}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/edn")))))

(deftest status-accepts-edn-response
  (testing "Status accepts EDN response format"
    (let [app (http/make-router (mock-adapters))
          resp (app {:request-method :get
                     :uri "/api/v1/status/00000000-0000-0000-0000-000000000001"
                     :path-params {:resource-id "00000000-0000-0000-0000-000000000001"}
                     :headers {"accept" "application/edn"}})]
      (is (= 200 (:status resp)))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/edn")))))

;; ---------------------------------------------------------------------------
;; Exception handling tests

(deftest exception-returns-problem-json
  (testing "Exceptions in handlers return problem+json"
    (let [error-adapters {:git {:repository (fn [_] (throw (ex-info "repo not found" {:code :not-found})))
                                :resolve-ref (constantly [])}
                          :repository-metadata {:list-repositories (constantly [])
                                                :register-repository (fn [_] (throw (ex-info "repo not found" {:code :not-found})))}
                          :observations {:list-checkpoints (constantly [])
                                         :list-ingestion-runs (constantly [])}
                          :index {:search (constantly [])
                                  :index-stats (constantly {:document-count 0})}
                          :embeddings {:embed-query (constantly [])}}
          app (http/make-router error-adapters)
          resp (app {:request-method :post
                     :uri "/api/v1/register"
                     :body-params {:path "/tmp/test"}
                     :headers {}})]
      (is (or (= 404 (:status resp))
              (= 400 (:status resp))))
      (is (.contains (get-in resp [:headers "Content-Type"]) "application/problem+json")))))
