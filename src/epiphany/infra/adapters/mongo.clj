(ns epiphany.infra.adapters.mongo
  "MongoDB adapter for Epiphany observations.

  Implements the observations port against a real MongoDB instance.
  This is the first :services-mode adapter; it establishes patterns
  for all future adapters.

  Collections:
    repository-location-v1  — repository location observations
    _index-versions         — tracks applied index versions

  The adapter uses a configurable database name (default: \"epiphany\").
  Integration tests use an isolated database (\"epiphany-test\") and clean
  only collections they own."

  (:require [epiphany.law.registry :as registry])
   (:import [com.mongodb.client MongoClients MongoDatabase MongoCollection]
           [com.mongodb.client.model IndexOptions Indexes]
           [com.mongodb MongoWriteException]
           [org.bson Document]
           [java.util Date]))

;; ---------------------------------------------------------------------------
;; Schema validation

(def ^:private validate-location-observation
  (registry/validator "observation/repository-location-v1"))

(defn- validate! [observation]
  (when-not (validate-location-observation observation)
    (let [explanation (registry/explain "observation/repository-location-v1" observation)]
      (throw (ex-info "Invalid repository-location observation"
                      {:code :schema-validation-failed
                       :explanation explanation
                       :observation observation})))))

;; ---------------------------------------------------------------------------
;; Document conversion

(defn- observation->doc
  "Convert a Clojure observation map to a MongoDB Document.
   Preserves exact path strings (no normalization).
   Keywords are stored as strings (via name) for MongoDB compatibility."
  [observation]
  (doto (Document.)
    (.put "_id" (str (:observation/id observation)))
    (.put "observation_type" (subs (str (:observation/type observation)) 1))
    (.put "request_id" (str (:observation/request-id observation)))
    (.put "observation_id" (str (:observation/id observation)))
    (.put "observed_at" (:observation/observed-at observation))
    (.put "adapter_version" (:observation/adapter-version observation))
    (.put "schema_version" (:observation/schema-version observation))
    (.put "resource_id" (str (:resource-id observation)))
    (.put "repository_path" (get-in observation [:repository/path :path/raw]))
    (.put "repository_path_source" (name (get-in observation [:repository/path :path/source])))
    (.put "common_git_dir" (get-in observation [:repository/common-git-dir :path/raw]))
    (.put "common_git_dir_source" (name (get-in observation [:repository/common-git-dir :path/source])))))

(defn- doc->observation
  "Convert a MongoDB Document back to a Clojure observation map.
   Preserves exact path strings."
  [^Document doc]
  {:observation/type           (keyword (.getString doc "observation_type"))
   :observation/request-id     (java.util.UUID/fromString (.getString doc "request_id"))
   :observation/id             (java.util.UUID/fromString (.getString doc "observation_id"))
   :observation/observed-at    (.getDate doc "observed_at")
   :observation/adapter-version (.getString doc "adapter_version")
   :observation/schema-version (.getLong doc "schema_version")
   :resource-id                (java.util.UUID/fromString (.getString doc "resource_id"))
   :repository/path            {:path/raw       (.getString doc "repository_path")
                                 :path/source    (keyword (.getString doc "repository_path_source"))
                                 :path/comparison :exact}
   :repository/common-git-dir  {:path/raw       (.getString doc "common_git_dir")
                                 :path/source    (keyword (.getString doc "common_git_dir_source"))
                                 :path/comparison :exact}})

;; ---------------------------------------------------------------------------
;; Index management

(def ^:private index-version
  "Current version of the repository-location-v1 indexes.
   Bump when index structure changes."
  1)

(defn ensure-indexes!
  "Create or update indexes for the repository-location-v1 collection.
   Versioned: skips if the current version is already applied."
  [conn]
  (let [collection (:repository-location-collection conn)
        index-versions (:index-versions-collection conn)
        current-doc (-> (.find index-versions)
                        (.filter (Document. "collection_name" "repository-location-v1"))
                        (.first))]
    (when (or (nil? current-doc)
              (let [v (.getLong current-doc "version")]
                (and v (< v (long index-version)))))
      ;; Unique index on (request_id) for idempotency
      (.createIndex collection
                    (Indexes/ascending (into-array String ["request_id"]))
                    (-> (IndexOptions.)
                        (.unique true)
                        (.name "request_id_unique_v1")))
      ;; Record the version
      (if current-doc
        (.updateOne index-versions
                     (.filter (Document. "collection_name" "repository-location-v1"))
                     (Document. "$set" (Document. "version" (long index-version))))
        (.insertOne index-versions
                    (doto (Document.)
                      (.put "collection_name" "repository-location-v1")
                       (.put "version" (long index-version))
                      (.put "applied_at" (Date.))))))))

;; ---------------------------------------------------------------------------
;; Connection management

(defn connect!
  "Connect to MongoDB and return a connection map.

   Options:
     :uri       — MongoDB connection URI (default: \"mongodb://127.0.0.1:27017\")
     :database  — database name (default: \"epiphany\")
     :test-mode — when true, uses \"epiphany-test\" database and cleans on connect"
  ([] (connect! {}))
  ([{:keys [uri database test-mode collection-prefix]
    :or   {uri "mongodb://127.0.0.1:27017"
           database "epiphany"}}]
   (let [db-name (if test-mode "epiphany-test" database)
         prefix  (or collection-prefix "")
         client  (MongoClients/create ^String uri)
         ^MongoDatabase db (.getDatabase client db-name)
         loc-coll (.getCollection db (str prefix "repository-location-v1"))
         run-coll (.getCollection db (str prefix "ingestion-run-v1"))
         ckpt-coll (.getCollection db (str prefix "projection-checkpoint-v1"))
         sect-coll (.getCollection db (str prefix "section-extraction-v1"))
         rev-coll (.getCollection db (str prefix "revision-at-path-v1"))
         idx-coll (.getCollection db (str prefix "_index-versions"))]
      (ensure-indexes! {:repository-location-collection loc-coll
                        :index-versions-collection idx-coll})
      {:client client
       :database db
       :db-name db-name
       :repository-location-collection loc-coll
       :ingestion-run-collection run-coll
       :projection-checkpoint-collection ckpt-coll
       :section-extraction-collection sect-coll
       :revision-at-path-collection rev-coll
       :index-versions-collection idx-coll})))

(defn disconnect!
  "Close the MongoDB connection."
  [{:keys [^com.mongodb.client.MongoClient client]}]
  (when client
    (.close client)))

(defn clean-test-db!
  "Drop all collections in the test database. Only for integration tests."
  [{:keys [^MongoCollection repository-location-collection
           ^MongoCollection ingestion-run-collection
           ^MongoCollection projection-checkpoint-collection
           ^MongoCollection section-extraction-collection
           ^MongoCollection revision-at-path-collection
           ^MongoCollection index-versions-collection]}]
  (.drop repository-location-collection)
  (.drop ingestion-run-collection)
  (.drop projection-checkpoint-collection)
  (.drop section-extraction-collection)
  (.drop revision-at-path-collection)
  (.drop index-versions-collection))

;; ---------------------------------------------------------------------------
;; Ingestion run document conversion

(defn- ingestion-run->doc
  "Convert an ingestion-run observation to a MongoDB Document."
  [observation]
  (doto (Document.)
    (.put "_id" (str (:observation/id observation)))
    (.put "observation_type" (subs (str (:observation/type observation)) 1))
    (.put "request_id" (str (:observation/request-id observation)))
    (.put "observation_id" (str (:observation/id observation)))
    (.put "observed_at" (:observation/observed-at observation))
    (.put "adapter_version" (:observation/adapter-version observation))
    (.put "schema_version" (:observation/schema-version observation))
    (.put "resource_id" (str (:resource-id observation)))
    (.put "repo_path" (get-in observation [:ingestion/repo-path :path/raw]))
    (.put "repo_path_source" (name (get-in observation [:ingestion/repo-path :path/source])))
    (.put "selected_refs" (mapv str (:ingestion/selected-refs observation)))
    (.put "commit_count" (long (:ingestion/commit-count observation)))
    (.put "failure_count" (long (:ingestion/failure-count observation)))
    (.put "failures" (mapv (fn [f]
                             (let [doc (Document.)]
                               (when-let [oid (:failure/oid f)]
                                 (.put doc "oid" (str oid)))
                               (when-let [ref (:failure/ref f)]
                                 (.put doc "ref" (name ref)))
                               (.put doc "reason" (:failure/reason f))
                               (when-let [msg (:failure/message f)]
                                 (.put doc "message" msg))
                               doc))
                           (:ingestion/failures observation)))))

(defn doc->ingestion-run
  "Convert a MongoDB Document back to an ingestion-run observation."
  [^Document doc]
  {:observation/type           (keyword (.getString doc "observation_type"))
   :observation/request-id     (when-let [s (.getString doc "request_id")]
                                 (java.util.UUID/fromString s))
   :observation/id             (java.util.UUID/fromString (.getString doc "observation_id"))
   :observation/observed-at    (.getDate doc "observed_at")
   :observation/adapter-version (.getString doc "adapter_version")
   :observation/schema-version (.getLong doc "schema_version")
   :resource-id                (java.util.UUID/fromString (.getString doc "resource_id"))
   :ingestion/repo-path        {:path/raw       (.getString doc "repo_path")
                                 :path/source    (keyword (.getString doc "repo_path_source"))
                                 :path/comparison :exact}
   :ingestion/selected-refs    (.getList doc "selected_refs")
   :ingestion/commit-count     (.getLong doc "commit_count")
   :ingestion/failure-count    (.getLong doc "failure_count")
   :ingestion/failures         (mapv (fn [^Document f]
                                       (cond-> {}
                                         (.containsKey f "oid")
                                         (assoc :failure/oid (java.util.UUID/fromString (.getString f "oid")))
                                         (.containsKey f "ref")
                                         (assoc :failure/ref (keyword (.getString f "ref")))
                                         :always
                                         (assoc :failure/reason (.getString f "reason"))
                                         (.containsKey f "message")
                                         (assoc :failure/message (.getString f "message"))))
                                     (.getList doc "failures"))})

;; ---------------------------------------------------------------------------
;; Projection checkpoint document conversion

(defn- checkpoint->doc
  "Convert a projection-checkpoint observation to a MongoDB Document."
  [observation]
  (doto (Document.)
    (.put "_id" (str (:observation/id observation)))
    (.put "observation_type" (subs (str (:observation/type observation)) 1))
    (.put "request_id" (str (:observation/request-id observation)))
    (.put "observation_id" (str (:observation/id observation)))
    (.put "observed_at" (:observation/observed-at observation))
    (.put "adapter_version" (:observation/adapter-version observation))
    (.put "schema_version" (:observation/schema-version observation))
    (.put "resource_id" (str (:resource-id observation)))
    (.put "projection_name" (:checkpoint/projection-name observation))
    (.put "projection_version" (long (:checkpoint/projection-version observation)))
    (.put "ingestion_run_id" (str (:checkpoint/ingestion-run-id observation)))
    (.put "status" (name (:checkpoint/status observation)))
    (cond-> (:checkpoint/last-processed-oid observation)
      (.put "last_processed_oid" (str (:checkpoint/last-processed-oid observation))))
    (.put "processed_count" (long (:checkpoint/processed-count observation)))
    (cond-> (:checkpoint/error-message observation)
      (.put "error_message" (:checkpoint/error-message observation)))))

(defn doc->checkpoint
  "Convert a MongoDB Document back to a projection-checkpoint observation."
  [^Document doc]
  (cond-> {:observation/type            (keyword (.getString doc "observation_type"))
           :observation/request-id      (when-let [s (.getString doc "request_id")]
                                          (java.util.UUID/fromString s))
           :observation/id              (java.util.UUID/fromString (.getString doc "observation_id"))
           :observation/observed-at     (.getDate doc "observed_at")
           :observation/adapter-version (.getString doc "adapter_version")
           :observation/schema-version  (.getLong doc "schema_version")
           :resource-id                 (java.util.UUID/fromString (.getString doc "resource_id"))
           :checkpoint/projection-name  (.getString doc "projection_name")
           :checkpoint/projection-version (.getLong doc "projection_version")
           :checkpoint/ingestion-run-id (java.util.UUID/fromString (.getString doc "ingestion_run_id"))
           :checkpoint/status           (keyword (.getString doc "status"))
           :checkpoint/processed-count  (.getLong doc "processed_count")}
    (.containsKey doc "last_processed_oid")
    (assoc :checkpoint/last-processed-oid (java.util.UUID/fromString (.getString doc "last_processed_oid")))
    (.containsKey doc "error_message")
    (assoc :checkpoint/error-message (.getString doc "error_message"))))

;; ---------------------------------------------------------------------------
;; Section extraction document conversion

(defn- section-extraction->doc
  "Convert a section-extraction observation to a MongoDB Document."
  [observation]
  (doto (Document.)
    (.put "_id" (str (:observation/id observation)))
    (.put "observation_type" (subs (str (:observation/type observation)) 1))
    (.put "request_id" (str (:observation/request-id observation)))
    (.put "observation_id" (str (:observation/id observation)))
    (.put "observed_at" (:observation/observed-at observation))
    (.put "adapter_version" (:observation/adapter-version observation))
    (.put "schema_version" (:observation/schema-version observation))
    (.put "resource_id" (str (:resource-id observation)))
    (.put "revision_at_path_id" (str (:extraction/revision-at-path-id observation)))
    (.put "commit_oid" (str (:extraction/commit-oid observation)))
    (.put "path_raw" (:extraction/path-raw observation))
    (.put "blob_oid" (str (:extraction/blob-oid observation)))
    (.put "extractor_version" (:extraction/extractor-version observation))
    (.put "section_count" (long (:extraction/section-count observation)))
    (.put "content_sha256" (:extraction/content-sha256 observation))
    (.put "sections" (mapv (fn [s]
                             (doto (Document.)
                               (.put "heading_path" (mapv str (:section/heading-path s)))
                               (.put "level" (long (:section/level s)))
                               (.put "ordinal" (long (:section/ordinal s)))
                               (.put "heading_span_start_byte" (long (:section/heading-span-start-byte s)))
                               (.put "heading_span_end_byte" (long (:section/heading-span-end-byte s)))
                               (.put "body_span_start_byte" (long (:section/body-span-start-byte s)))
                               (.put "body_span_end_byte" (long (:section/body-span-end-byte s)))
                               (.put "body_span_start_line" (long (:section/body-span-start-line s)))
                               (.put "body_span_end_line" (long (:section/body-span-end-line s)))))
                           (:extraction/sections observation)))))

(defn doc->section-extraction
  "Convert a MongoDB Document back to a section-extraction observation."
  [^Document doc]
  {:observation/type            (keyword (.getString doc "observation_type"))
   :observation/request-id     (when-let [s (.getString doc "request_id")]
                                 (java.util.UUID/fromString s))
   :observation/id             (java.util.UUID/fromString (.getString doc "observation_id"))
   :observation/observed-at    (.getDate doc "observed_at")
   :observation/adapter-version (.getString doc "adapter_version")
   :observation/schema-version (.getLong doc "schema_version")
   :resource-id                (java.util.UUID/fromString (.getString doc "resource_id"))
   :extraction/revision-at-path-id (java.util.UUID/fromString (.getString doc "revision_at_path_id"))
   :extraction/commit-oid      (.getString doc "commit_oid")
   :extraction/path-raw        (.getString doc "path_raw")
   :extraction/blob-oid        (.getString doc "blob_oid")
   :extraction/extractor-version (.getString doc "extractor_version")
   :extraction/section-count   (.getLong doc "section_count")
   :extraction/content-sha256  (.getString doc "content_sha256")
   :extraction/sections        (mapv (fn [^Document s]
                                       {:section/heading-path            (vec (.getList s "heading_path"))
                                        :section/level                   (.getLong s "level")
                                        :section/ordinal                 (.getLong s "ordinal")
                                        :section/heading-span-start-byte (.getLong s "heading_span_start_byte")
                                        :section/heading-span-end-byte   (.getLong s "heading_span_end_byte")
                                        :section/body-span-start-byte    (.getLong s "body_span_start_byte")
                                        :section/body-span-end-byte      (.getLong s "body_span_end_byte")
                                        :section/body-span-start-line    (.getLong s "body_span_start_line")
                                        :section/body-span-end-line      (.getLong s "body_span_end_line")})
                                     (.getList doc "sections"))})

;; ---------------------------------------------------------------------------
;; Revision-at-path document conversion

(defn- revision-at-path->doc
  "Convert a revision-at-path observation to a MongoDB Document."
  [observation]
  (doto (Document.)
    (.put "_id" (str (:observation/id observation)))
    (.put "observation_type" (subs (str (:observation/type observation)) 1))
    (.put "request_id" (str (:observation/request-id observation)))
    (.put "observation_id" (str (:observation/id observation)))
    (.put "observed_at" (:observation/observed-at observation))
    (.put "adapter_version" (:observation/adapter-version observation))
    (.put "schema_version" (:observation/schema-version observation))
    (.put "resource_id" (str (:resource-id observation)))
    (.put "revision_at_path_id" (str (:revision-at-path/id observation)))
    (.put "commit_oid" (:revision/commit-oid observation))
    (.put "tree_oid" (:revision/tree-oid observation))
    (.put "path_raw" (:revision/path-raw observation))
    (.put "blob_oid" (:revision/blob-oid observation))
    (.put "mode" (long (:revision/mode observation)))
    (.put "evidence" (name (:revision/evidence observation)))
    (cond-> (:revision/parent-commit-oid observation)
      (.put "parent_commit_oid" (:revision/parent-commit-oid observation)))
    (cond-> (:revision/parent-blob-oid observation)
      (.put "parent_blob_oid" (:revision/parent-blob-oid observation)))))

(defn doc->revision-at-path
  "Convert a MongoDB Document back to a revision-at-path observation."
  [^Document doc]
  (cond-> {:observation/type            (keyword (.getString doc "observation_type"))
           :observation/request-id      (when-let [s (.getString doc "request_id")]
                                          (java.util.UUID/fromString s))
           :observation/id              (java.util.UUID/fromString (.getString doc "observation_id"))
           :observation/observed-at     (.getDate doc "observed_at")
           :observation/adapter-version (.getString doc "adapter_version")
           :observation/schema-version  (.getLong doc "schema_version")
           :resource-id                 (java.util.UUID/fromString (.getString doc "resource_id"))
           :revision-at-path/id        (java.util.UUID/fromString (.getString doc "revision_at_path_id"))
           :revision/commit-oid        (.getString doc "commit_oid")
           :revision/tree-oid          (.getString doc "tree_oid")
           :revision/path-raw          (.getString doc "path_raw")
           :revision/blob-oid          (.getString doc "blob_oid")
           :revision/mode              (.getLong doc "mode")
           :revision/evidence          (keyword (.getString doc "evidence"))}
    (.containsKey doc "parent_commit_oid")
    (assoc :revision/parent-commit-oid (.getString doc "parent_commit_oid"))
    (.containsKey doc "parent_blob_oid")
    (assoc :revision/parent-blob-oid (.getString doc "parent_blob_oid"))))

;; ---------------------------------------------------------------------------
;; Adapter implementation

(defn- doc->observation-equal?
  "Check if two observation maps are semantically equal (ignoring envelope fields
   that differ between writes)."
  [a b]
  (and (= (:resource-id a) (:resource-id b))
       (= (get-in a [:repository/path :path/raw])
          (get-in b [:repository/path :path/raw]))
       (= (get-in a [:repository/common-git-dir :path/raw])
          (get-in b [:repository/common-git-dir :path/raw]))))

(defn make-observations-adapter
  "Create an observations port backed by MongoDB.
   Returns a map matching the observations port schema."
  [conn]
  (letfn [(insert-or-idempotent! [observation]
            (validate! observation)
            (let [^MongoCollection coll (:repository-location-collection conn)
                  request-id (str (:observation/request-id observation))
                  doc        (observation->doc observation)]
              (try
                (.insertOne coll doc)
                :inserted
                (catch MongoWriteException e
                  (let [code (.getCode e)]
                    (if (= 11000 code)
                      (let [existing (-> (.find coll)
                                          (.filter (Document. "request_id" request-id))
                                         (.first))]
                        (if (doc->observation-equal? (doc->observation existing) observation)
                          :idempotent
                          (throw (ex-info "Idempotency conflict: same request-id with different content"
                                          {:code :idempotency/conflict
                                           :request-id (:observation/request-id observation)
                                           :existing (doc->observation existing)
                                           :submitted observation}))))
                      (throw e)))))))]
    {:find-by-request-id
     (fn [request-id]
       (let [^MongoCollection coll (:repository-location-collection conn)
             doc (-> (.find coll)
                      (.filter (Document. "request_id" (str request-id)))
                     (.first))]
         (when doc
           (doc->observation doc))))

     :record-repository-location!
     insert-or-idempotent!

     :record-ingestion-run!
     (fn [observation]
       (let [^MongoCollection coll (:ingestion-run-collection conn)]
         (.insertOne coll (ingestion-run->doc observation))
         nil))

      :record-checkpoint!
      (fn [observation]
        (let [^MongoCollection coll (:projection-checkpoint-collection conn)]
          (.insertOne coll (checkpoint->doc observation))
          nil))

      :record-section-extraction!
      (fn [observation]
        (let [^MongoCollection coll (:section-extraction-collection conn)]
          (.insertOne coll (section-extraction->doc observation))
          nil))

      :record-revision-at-path!
      (fn [observation]
        (let [^MongoCollection coll (:revision-at-path-collection conn)]
          (.insertOne coll (revision-at-path->doc observation))
          nil))

      :list-ingestion-runs
     (fn [resource-id]
       (let [^MongoCollection coll (:ingestion-run-collection conn)
             docs (-> (.find coll)
                      (.filter (Document. "resource_id" (str resource-id)))
                      (.into (java.util.ArrayList.)))]
         (mapv doc->ingestion-run docs)))

      :list-checkpoints
     (fn [ingestion-run-id]
       (let [^MongoCollection coll (:projection-checkpoint-collection conn)
             docs (-> (.find coll)
                      (.filter (Document. "ingestion_run_id" (str ingestion-run-id)))
                      (.into (java.util.ArrayList.)))]
         (mapv doc->checkpoint docs)))

      :list-revision-at-path-by-resource
     (fn [resource-id]
       (let [^MongoCollection coll (:revision-at-path-collection conn)
             docs (-> (.find coll)
                      (.filter (Document. "resource_id" (str resource-id)))
                      (.into (java.util.ArrayList.)))]
         (mapv doc->revision-at-path docs)))

      :list-section-extractions-by-revision
     (fn [revision-at-path-id]
       (let [^MongoCollection coll (:section-extraction-collection conn)
             docs (-> (.find coll)
                      (.filter (Document. "revision_at_path_id" (str revision-at-path-id)))
                      (.into (java.util.ArrayList.)))]
         (mapv doc->section-extraction docs)))}))
