(ns epiphany.infra.adapters.lucene
  "Lucene adapter for the index port.

  Implements lexical search and KNN vector search against a Lucene
  directory. The index is fully rebuildable — a rebuild drops and
  recreates all segments. Index version is tracked in a sidecar file
  so schema changes are detectable.

  Two document types coexist in the same index:

  Section documents (doc_type=section):
    - heading_path, path_raw, body_text, commit_oid, blob_oid,
      extractor_version, section_level, section_ordinal

  Embedding documents (doc_type=embedding):
    - embedding_path_raw, embedding_commit_oid, embedding_heading_path,
      embedding_level, embedding_ordinal, embedding_model,
      embedding_version (stored int for filtering), knn_vector (KNN float)

  Uses StandardAnalyzer for text analysis — sufficient for English
  prose and code comments. Unicode paths are stored verbatim."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [org.apache.lucene.analysis.standard StandardAnalyzer]
           [org.apache.lucene.document Document Field$Store TextField StringField
                                       KnnFloatVectorField]
           [org.apache.lucene.index IndexWriter IndexWriterConfig IndexWriterConfig$OpenMode
                                    DirectoryReader Term VectorSimilarityFunction]
           [org.apache.lucene.queryparser.classic QueryParser]
           [org.apache.lucene.search IndexSearcher ScoreDoc TermQuery BooleanQuery
                                      BooleanQuery$Builder BooleanClause$Occur]
           [org.apache.lucene.store FSDirectory]
           [java.nio.file Files Path]))

;; ---------------------------------------------------------------------------
;; Index version

(def ^:private current-index-version
  "Bump when the index schema (fields, analyzers) changes."
  2)

;; ---------------------------------------------------------------------------
;; Document construction — sections

(defn- section->doc
  "Convert a section extraction record + section map into a Lucene Document."
  [extraction section]
  (let [doc (Document.)]
    (.add doc (StringField. "doc_type" "section" Field$Store/YES))
    (.add doc (StringField. "path_raw"
                            (:extraction/path-raw extraction)
                            Field$Store/YES))
    (.add doc (StringField. "commit_oid"
                            (str (:extraction/commit-oid extraction))
                            Field$Store/YES))
    (.add doc (StringField. "blob_oid"
                            (str (:extraction/blob-oid extraction))
                            Field$Store/YES))
    (.add doc (StringField. "extractor_version"
                            (:extraction/extractor-version extraction)
                            Field$Store/YES))
    (.add doc (TextField. "heading_path"
                          (str/join " " (:section/heading-path section))
                          Field$Store/YES))
    (.add doc (StringField. "section_level"
                            (str (:section/level section))
                            Field$Store/YES))
    (.add doc (StringField. "section_ordinal"
                            (str (:section/ordinal section))
                            Field$Store/YES))
    (.add doc (TextField. "body_text"
                          (str/join " " (concat (:section/heading-path section)
                                                [(:extraction/path-raw extraction)]
                                                (when-let [body (:section/body section)]
                                                  [body])))
                          Field$Store/YES))
    doc))

(defn- extraction-record->docs
  "Expand a section extraction record into one Lucene Document per section."
  [record]
  (mapv (fn [section] (section->doc record section))
        (:extraction/sections record)))

;; ---------------------------------------------------------------------------
;; Document construction — embeddings

(defn- embedding->doc
  "Convert an embedding record into a Lucene Document with a KNN vector field."
  [embedding]
  (let [vector (:embedding/vector embedding)
        float-vec (float-array vector)
        doc (Document.)]
    (.add doc (StringField. "doc_type" "embedding" Field$Store/YES))
    (.add doc (StringField. "embedding_path_raw"
                            (:embedding/path-raw embedding)
                            Field$Store/YES))
    (.add doc (StringField. "embedding_commit_oid"
                            (str (:embedding/commit-oid embedding))
                            Field$Store/YES))
    (.add doc (TextField. "embedding_heading_path"
                          (str/join " " (:embedding/heading-path embedding))
                          Field$Store/YES))
    (.add doc (StringField. "embedding_level"
                            (str (:embedding/level embedding))
                            Field$Store/YES))
    (.add doc (StringField. "embedding_ordinal"
                            (str (:embedding/ordinal embedding))
                            Field$Store/YES))
    (.add doc (StringField. "embedding_model"
                            (:embedding/model embedding)
                            Field$Store/YES))
    (.add doc (StringField. "embedding_version"
                            (str (:embedding-version embedding))
                            Field$Store/YES))
    ;; KNN vector field — COSINE similarity, stored for retrieval
    (.add doc (KnnFloatVectorField. "knn_vector" float-vec
                                    VectorSimilarityFunction/COSINE))
    doc))

;; ---------------------------------------------------------------------------
;; Index writer management

(defn- open-writer
  "Open or create an IndexWriter at the given directory path."
  [^Path dir]
  (let [analyzer (StandardAnalyzer.)
        config (IndexWriterConfig. analyzer)]
    (.setOpenMode config IndexWriterConfig$OpenMode/CREATE_OR_APPEND)
    (IndexWriter. (FSDirectory/open dir) config)))

(defn- write-version-file!
  "Write the current index version to a sidecar file."
  [^Path index-dir]
  (let [version-file (.resolve index-dir "index-version.edn")]
    (spit (.toFile version-file) (str {:index/version current-index-version}))))

(defn- read-version-file
  "Read the index version from the sidecar file: nil if missing,
  :integrity/corrupt-version-file if present but unparseable (ENG-017K) —
  never an exception escaping the adapter."
  [^Path index-dir]
  (let [version-file (.resolve index-dir "index-version.edn")]
    (when (.exists (.toFile version-file))
      (try
        (edn/read-string {} (slurp (.toFile version-file)))
        (catch Exception _ :integrity/corrupt-version-file)))))

(defn- index-empty?
  "Check if the Lucene index directory has any segment files."
  [^Path index-dir]
  (let [dir (java.io.File. (.toString index-dir))
        segment-files (filter #(.startsWith (.getName %) "segments_")
                               (.listFiles dir))]
    (empty? segment-files)))

;; ---------------------------------------------------------------------------
;; Query helpers

(defn- doc-type-query
  "Build a TermQuery for filtering by doc_type."
  [doc-type]
  (TermQuery. (Term. "doc_type" doc-type)))

(defn- version-filter-query
  "Build a TermQuery for filtering by embedding_version."
  [version]
  (TermQuery. (Term. "embedding_version" (str version))))

;; ---------------------------------------------------------------------------
;; Port implementation

(defn make-index-adapter
  "Create an index port backed by a Lucene directory.

   Options:
     :index-dir — java.nio.file.Path to the index directory (required)"
  [{:keys [^Path index-dir]}]
  (when-not index-dir
    (throw (ex-info "Lucene adapter requires :index-dir" {})))
  (Files/createDirectories index-dir (into-array java.nio.file.attribute.FileAttribute []))

  {:index-sections!
   (fn [extraction-record]
     (let [docs (extraction-record->docs extraction-record)]
       (with-open [writer (open-writer index-dir)]
         (doseq [^Document doc docs]
           (.addDocument writer doc))
         (.commit writer)))
     (write-version-file! index-dir)
     nil)

   :search
   (fn [query-string]
     (if (index-empty? index-dir)
       []
       (let [analyzer (StandardAnalyzer.)]
         (with-open [reader (DirectoryReader/open (FSDirectory/open index-dir))]
           (let [searcher (IndexSearcher. reader)
                 sf (.storedFields searcher)
                 parser (QueryParser. "body_text" analyzer)
                 query (.parse parser query-string)
                 ;; Combine lexical query with doc_type=section filter
                 combined (-> (BooleanQuery$Builder.)
                              (.add query BooleanClause$Occur/MUST)
                              (.add (doc-type-query "section") BooleanClause$Occur/FILTER)
                              .build)
                 hits (.search searcher combined 100)]
             (mapv (fn [^ScoreDoc hit]
                     (let [doc (.document sf (.-doc hit))]
                       {:result/path-raw (.get doc "path_raw")
                        :result/commit-oid (.get doc "commit_oid")
                        :result/heading-path (vec (str/split (.get doc "heading_path") #" "))
                        :result/score (.-score hit)}))
                   (.-scoreDocs hits)))))))

   :index-embeddings!
   (fn [embedding-records]
     (let [docs (mapv embedding->doc embedding-records)]
       (with-open [writer (open-writer index-dir)]
         (doseq [^Document doc docs]
           (.addDocument writer doc))
         (.commit writer)))
     nil)

   :knn-search
   (fn [{:keys [vector k embedding-version]}]
     (if (index-empty? index-dir)
       []
       (let [float-vec (float-array vector)
             base-query (KnnFloatVectorField/newVectorQuery "knn_vector" float-vec (or k 10))
             builder (doto (BooleanQuery$Builder.)
                       (.add base-query BooleanClause$Occur/MUST)
                       (.add (doc-type-query "embedding") BooleanClause$Occur/FILTER))
             _ (when embedding-version
                 (.add builder (version-filter-query embedding-version) BooleanClause$Occur/FILTER))
             combined (.build builder)]
         (with-open [reader (DirectoryReader/open (FSDirectory/open index-dir))]
           (let [searcher (IndexSearcher. reader)
                 sf (.storedFields searcher)
                 hits (.search searcher combined (or k 10))]
             (mapv (fn [^ScoreDoc hit]
                     (let [doc (.document sf (.-doc hit))]
                       {:result/path-raw (.get doc "embedding_path_raw")
                        :result/commit-oid (.get doc "embedding_commit_oid")
                        :result/heading-path (vec (str/split (.get doc "embedding_heading_path") #" "))
                        :result/score (.-score hit)
                        :result/model (.get doc "embedding_model")
                        :result/embedding-version (.get doc "embedding_version")}))
                   (.-scoreDocs hits)))))))

   :index-version
   (fn []
     (let [result (read-version-file index-dir)]
       (cond
         (= result :integrity/corrupt-version-file) result
         (map? result) (or (:index/version result) 0)
         :else 0)))

   :rebuild-index!
   (fn [records]
     (with-open [writer (open-writer index-dir)]
       (.deleteAll writer)
       (.commit writer))
     (with-open [writer (open-writer index-dir)]
       (doseq [record records]
         (let [docs (extraction-record->docs record)]
           (doseq [^Document doc docs]
             (.addDocument writer doc))))
       (.commit writer))
     (write-version-file! index-dir)
     nil)

   :clear-index!
   (fn []
     (with-open [writer (open-writer index-dir)]
       (.deleteAll writer)
       (.commit writer))
     (let [version-file (.resolve index-dir "index-version.edn")]
       (when (.exists (.toFile version-file))
         (.delete (.toFile version-file))))
     nil)})
