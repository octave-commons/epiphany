(ns epiphany.infra.adapters.ollama
  "Ollama HTTP adapter for the embeddings port.

  Calls the local Ollama API (default localhost:11434) for dense vector
  embeddings using the /api/embed endpoint. Embeddings are L2-normalized
  by Ollama, so cosine similarity is a simple dot product.

  The adapter tracks the model name and dimensions so the embedding
  version can be computed deterministically from configuration."
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [epiphany.shape.markdown :as md])
  (:import [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse HttpResponse$BodyHandlers]
           [java.net URI]
           [java.time Duration]))

(defn- make-http-client []
  (HttpClient/newHttpClient))

(defn- embed-request
  "Call Ollama /api/embed with a model and input texts.
   Returns the parsed JSON response map."
  [^HttpClient client base-url model input-texts opts]
  (let [body (json/write-str (cond-> {:model model
                                      :input input-texts}
                                 (:truncate opts) (assoc :truncate (:truncate opts))
                                 (:dimensions opts) (assoc :dimensions (:dimensions opts))
                                 (:keep-alive opts) (assoc :keep_alive (:keep-alive opts))))
        request (-> (HttpRequest/newBuilder)
                    (.uri (URI. (str base-url "/api/embed")))
                    (.POST (HttpRequest$BodyPublishers/ofString body))
                    (.header "Content-Type" "application/json")
                    (.timeout (Duration/ofSeconds 120))
                    .build)
        response (.send client request (HttpResponse$BodyHandlers/ofString))]
    (when-not (= 200 (.statusCode response))
      (throw (ex-info (str "Ollama embed failed: " (.statusCode response))
                      {:status (.statusCode response)
                       :body (.body response)})))
    (json/read-str (.body response) :key-fn keyword)))

(defn- model-digest-request
  "Resolve the immutable digest for `model` from Ollama's /api/tags API."
  [^HttpClient client base-url model]
  (let [request (-> (HttpRequest/newBuilder)
                    (.uri (URI. (str base-url "/api/tags")))
                    (.GET)
                    (.timeout (Duration/ofSeconds 30))
                    .build)
        response (.send client request (HttpResponse$BodyHandlers/ofString))]
    (when-not (= 200 (.statusCode response))
      (throw (ex-info (str "Ollama model metadata failed: " (.statusCode response))
                      {:status (.statusCode response)
                       :body (.body response)})))
    (let [models (:models (json/read-str (.body response) :key-fn keyword))
          aliases #{model
                    (str model ":latest")
                    (str/replace model #":latest$" "")}
          exact (some #(when (or (contains? aliases (:name %))
                                 (contains? aliases (:model %)))
                         %)
                      models)
          digest (:digest exact)]
      (when (str/blank? digest)
        (throw (ex-info (str "Ollama model digest unavailable for " model)
                        {:code :model-digest-unavailable
                         :model model})))
      digest)))

(defn- section-body-text
  [extraction section]
  (when-let [content (:extraction/content extraction)]
    (let [start (:section/body-span-start-byte section)
          end (:section/body-span-end-byte section)]
      (when (and start end)
        (md/slice content {:span/start-byte start :span/end-byte end})))))

(defn- section-input
  "Build one embedding input from the exact historical section body plus
   enough heading/path context to keep short sections discoverable."
  [extraction section]
  {:text (str/join
          "\n"
          (remove str/blank?
                  [(str/join " > " (:section/heading-path section))
                   (:extraction/path-raw extraction)
                   (section-body-text extraction section)]))
   :resource-id (:resource-id extraction)
   :extraction-path-raw (:extraction/path-raw extraction)
   :extraction-commit-oid (:extraction/commit-oid extraction)
   :section/heading-path (:section/heading-path section)
   :section/level (:section/level section)
   :section/ordinal (:section/ordinal section)})

(defn make-embeddings-adapter
  "Create an embeddings port backed by Ollama HTTP.

   Options:
     :base-url  — Ollama server URL (default: \"http://localhost:11434\")
     :model     — embedding model name (default: \"nomic-embed-text\")
     :dimensions — output dimensions, nil for model default (default: nil)
     :model-digest — optional immutable digest override (tests/offline pinning)
     :batch-size — texts per embed request (default: 64)"
  [{:keys [base-url model dimensions model-digest batch-size]
    :or {base-url "http://localhost:11434"
         model "nomic-embed-text"
         batch-size 64}}]
  (let [client (make-http-client)
        resolved-model-digest
        (delay (or model-digest
                   (model-digest-request client base-url model)))
        version (delay (hash {:model model
                              :digest @resolved-model-digest
                              :dimensions dimensions}))]
    {:embed-sections!
     (fn [extraction-records]
       (let [embedding-version @version
             ;; Build section texts from extraction records
             section-inputs
             (mapcat (fn [rec]
                       (map #(section-input rec %) (:extraction/sections rec)))
                     extraction-records)
             ;; Batch embed
             batches (partition-all batch-size section-inputs)
             results (atom [])]
         (doseq [batch batches]
           (let [texts (mapv :text batch)
                 resp (embed-request client base-url model texts
                                     {:truncate true
                                      :dimensions dimensions})
                 embeddings (:embeddings resp)]
             (when-not (= (count texts) (count embeddings))
               (throw (ex-info "Embed count mismatch"
                               {:expected (count texts)
                                :actual (count embeddings)})))
             (swap! results into
                    (map (fn [input embedding]
                           {:resource-id (:resource-id input)
                            :embedding/path-raw (:extraction-path-raw input)
                            :embedding/commit-oid (:extraction-commit-oid input)
                            :embedding/heading-path (:section/heading-path input)
                            :embedding/level (:section/level input)
                            :embedding/ordinal (:section/ordinal input)
                            :embedding/vector embedding
                            :embedding/model model
                            :embedding/model-digest @resolved-model-digest
                            :embedding/dimensions (count embedding)
                            :embedding-version embedding-version})
                         batch embeddings))))
         @results))

     :embed-query
     (fn [text]
       (let [resp (embed-request client base-url model [text]
                                 {:truncate true
                                  :dimensions dimensions})]
         (first (:embeddings resp))))

     :embedding-version
     (fn [] @version)

     :clear-embeddings!
     (fn [] nil)}))

(defn available?
  "TCP probe for a local Ollama service (explicit, never silent).
   Used by the command seam to return UNAVAILABLE rather than letting a
   connection error escape as a generic fault."
  ([]
   (available? {:host "127.0.0.1" :port 11434 :timeout-ms 2000}))
  ([{:keys [host port timeout-ms]}]
   (try
     (with-open [sock (java.net.Socket.)]
       (.connect sock (java.net.InetSocketAddress. ^String host ^int port) ^int timeout-ms)
       true)
     (catch Exception _ false))))
