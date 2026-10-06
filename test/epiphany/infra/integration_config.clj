(ns epiphany.infra.integration-config
  "Explicit test-only settings for disposable service integration fixtures."
  (:require [clojure.string :as str])
  (:import [java.net URI]))

(defn- setting [name fallback]
  (let [value (System/getenv name)]
    (if (str/blank? value) fallback value)))

(defn mongo-options
  "Require an explicitly selected Mongo instance and isolate fixture collections."
  []
  (let [uri (setting "EPIPHANY_TEST_MONGODB_URI" nil)]
    (when-not uri
      (throw (ex-info "Integration requires EPIPHANY_TEST_MONGODB_URI; no ambient database is selected"
                      {:code :integration-service-unconfigured :service :mongodb})))
    {:uri uri :database "epiphany_integration"}))

(defn embedding-dimensions
  "Expected real model dimensions; no padding or truncation is used by the tests."
  []
  (let [dimensions (parse-long (setting "EPIPHANY_TEST_EMBEDDING_DIMENSIONS" "768"))]
    (when-not (and dimensions (pos? dimensions))
      (throw (ex-info "EPIPHANY_TEST_EMBEDDING_DIMENSIONS must be a positive integer"
                      {:code :invalid-integration-configuration})))
    dimensions))

(defn embedding-options
  "Keep the original Ollama model defaults while allowing a pinned protocol provider."
  []
  (cond-> {:base-url (setting "EPIPHANY_TEST_EMBEDDING_BASE_URL" "http://localhost:11434")
           :model (setting "EPIPHANY_TEST_EMBEDDING_MODEL" "nomic-embed-text")
           :dimensions (embedding-dimensions)}
    (setting "EPIPHANY_TEST_EMBEDDING_DIGEST" nil)
    (assoc :model-digest (setting "EPIPHANY_TEST_EMBEDDING_DIGEST" nil))))

(defn- endpoint [address default-port]
  (let [uri (URI. address)
        port (.getPort uri)]
    (when-not (.getHost uri)
      (throw (ex-info "Integration endpoints must name one explicit service host"
                      {:code :invalid-integration-configuration})))
    {:host (.getHost uri) :port (if (neg? port) default-port port) :timeout-ms 2000}))

(defn readiness-options
  "Check the same Mongo and S3 endpoints selected by the integration supervisor."
  []
  {:mongodb (endpoint (:uri (mongo-options)) 27017)
   :s3 (endpoint (setting "EPIPHANY_TEST_S3_ENDPOINT" "http://127.0.0.1:9000") 9000)})
