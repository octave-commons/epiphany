(ns epiphany.infra.adapters.ollama
  "Ollama HTTP adapter for the embeddings port.

  Calls the local Ollama API (default localhost:11434) for dense vector
  embeddings using the /api/embed endpoint. Embeddings are L2-normalized
  by Ollama, so cosine similarity is a simple dot product.

  The adapter tracks the model name and dimensions so the embedding
  version can be computed deterministically from configuration."
  (:require [clojure.string :as str]
            [clojure.data.json :as json])
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

(defn make-embeddings-adapter
  "Create an embeddings port backed by Ollama HTTP.

   Options:
     :base-url  — Ollama server URL (default: \"http://localhost:11434\")
     :model     — embedding model name (default: \"nomic-embed-text\")
     :dimensions — output dimensions, nil for model default (default: nil)
     :batch-size — texts per embed request (default: 64)"
  [{:keys [base-url model dimensions batch-size]
    :or {base-url "http://localhost:11434"
         model "nomic-embed-text"
         batch-size 64}}]
  (let [client (make-http-client)
        version (hash {:model model :dimensions dimensions})]
    {:embed-sections!
     (fn [extraction-records]
       (let [ ;; Build section texts from extraction records
             section-inputs
             (mapcat (fn [rec]
                       (map (fn [s]
                              {:text (str/join " " (concat (:section/heading-path s)
                                                          [(:extraction/path-raw rec)]))
                               :extraction-path-raw (:extraction/path-raw rec)
                               :extraction-commit-oid (:extraction/commit-oid rec)
                               :section/heading-path (:section/heading-path s)
                               :section/level (:section/level s)
                               :section/ordinal (:section/ordinal s)})
                            (:extraction/sections rec)))
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
                           {:embedding/path-raw (:extraction-path-raw input)
                            :embedding/commit-oid (:extraction-commit-oid input)
                            :embedding/heading-path (:section/heading-path input)
                            :embedding/level (:section/level input)
                            :embedding/ordinal (:section/ordinal input)
                            :embedding/vector embedding
                            :embedding/model model
                            :embedding/dimensions (count embedding)})
                         batch embeddings))))
         @results))

     :embed-query
     (fn [text]
       (let [resp (embed-request client base-url model [text]
                                 {:truncate true
                                  :dimensions dimensions})]
         (first (:embeddings resp))))

     :embedding-version
     (fn [] version)

     :clear-embeddings!
     (fn [] nil)}))
