(ns epiphany.extern.resource
  "Bounded safe parsers for selected historical bytes, not acquisition or agent execution."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [epiphany.law.resource :as law]
            [malli.core :as m]
            [epiphany.shape.conversations :as conversations])
  (:import [com.fasterxml.jackson.core JsonFactory JsonParser$Feature]
           [com.fasterxml.jackson.databind ObjectMapper DeserializationFeature]
           [java.nio ByteBuffer]
           [java.security MessageDigest]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.util Base64 Map List Set Date UUID]
           [org.tomlj Toml TomlTable TomlArray]
           [org.yaml.snakeyaml Yaml LoaderOptions]
           [org.yaml.snakeyaml.constructor SafeConstructor]))

(defrecord TemporalValue [kind literal])

(def max-source-bytes "Per selected text resource; exceeding it is an explicit refusal." 1048576)

(defn format-for
  "Classify by explicit source extension; no content-sniffed executable readers."
  [path]
  (get {"json" :json "jsonc" :jsonc "jsonl" :jsonl "edn" :edn "toml" :toml
        "yaml" :yaml "yml" :yaml} (last (str/split (str/lower-case path) #"\.")) :unsupported))

(defn- parser-version [format]
  (case format
    (:json :jsonc :jsonl) (str "jackson/" com.fasterxml.jackson.core.json.PackageVersion/VERSION)
    :edn (str "clojure.edn/" (clojure-version))
    :yaml "snakeyaml/2.3-safe"
    :toml "tomlj/1.1.1"
    "unsupported"))

(defn- decode-json [source comments?]
  (let [factory (doto (JsonFactory.) (.enable JsonParser$Feature/STRICT_DUPLICATE_DETECTION))]
    (when comments?
      (.enable factory JsonParser$Feature/ALLOW_COMMENTS)
      (.enable factory JsonParser$Feature/ALLOW_TRAILING_COMMA))
    (with-open [parser (.createParser factory ^String source)]
      (let [mapper (doto (ObjectMapper. factory)
                     (.enable DeserializationFeature/USE_BIG_DECIMAL_FOR_FLOATS)
                     (.enable DeserializationFeature/USE_BIG_INTEGER_FOR_INTS))
            value (.readValue mapper parser Object)]
        (when (.nextToken parser) (throw (ex-info "Trailing JSON value" {:code :malformed})))
        value))))

(defn- decode-edn [source]
  (with-open [reader (java.io.PushbackReader. (java.io.StringReader. source))]
    (let [options {:eof ::eof :readers {} :default (fn [_ _] (throw (ex-info "Custom EDN tags forbidden" {:code :malformed})))}
          value (edn/read options reader)]
      (when (or (= ::eof value) (not= ::eof (edn/read options reader)))
        (throw (ex-info "Expected one EDN value" {:code :malformed})))
      value)))

(defn- decode-yaml [source]
  (let [options (doto (LoaderOptions.) (.setAllowDuplicateKeys false)
                      (.setMaxAliasesForCollections 0) (.setNestingDepthLimit 64)
                      (.setCodePointLimit max-source-bytes))]
    (.load (Yaml. (SafeConstructor. options)) ^String source)))

(defn- portable
  "Decode foreign collection values; retain TOML local date/time types as tagged data."
  [value]
  (cond
    (instance? TomlTable value) (into {} (map (fn [key] [key (portable (.get ^TomlTable value ^java.util.List [key]))]) (.keySet ^TomlTable value)))
    (instance? TomlArray value) (mapv #(portable (.get ^TomlArray value (int %))) (range (.size ^TomlArray value)))
    (list? value) (apply list (map portable value))
    (instance? Map value) (into {} (map (fn [[k v]] [(portable k) (portable v)]) value))
    (or (instance? List value) (vector? value)) (mapv portable value)
    (instance? Set value) (set (map portable value))
    (instance? java.time.temporal.TemporalAccessor value)
    (->TemporalValue (keyword (.getSimpleName (class value))) (str value))
    :else value))

(defn- value-tree [value span depth]
  (when (> depth 64) (throw (ex-info "Resource nesting limit" {:code :malformed})))
  (let [base {:value/span span} child #(value-tree % span (inc depth))]
    (merge base
           (cond
             (nil? value) {:value/type :null}
             (instance? TemporalValue value) {:value/type (:kind value) :value/data (:literal value)}
             (map? value) {:value/type :map :value/entries (mapv (fn [[k v]] {:entry/key (child k) :entry/value (child v)}) value)}
             (vector? value) {:value/type :vector :value/items (mapv child value)}
             (list? value) {:value/type :list :value/items (mapv child value)}
             (set? value) {:value/type :set :value/items (mapv child (sort-by pr-str value))}
             (instance? Date value) {:value/type :instant :value/data (str (.toInstant ^Date value))}
             (instance? UUID value) {:value/type :uuid :value/data (str value)}
             (keyword? value) {:value/type :keyword :value/data (str value)}
             (symbol? value) {:value/type :symbol :value/data (str value)}
             (ratio? value) {:value/type :ratio :value/data (str value)}
             (decimal? value) {:value/type :decimal :value/data (str value)}
             (integer? value) {:value/type :integer :value/data (str value)}
             (number? value) {:value/type :float :value/data (str value)}
             (boolean? value) {:value/type :boolean :value/data value}
             (string? value) {:value/type :string :value/data value}
             (char? value) {:value/type :character :value/data (str value)}
             :else (throw (ex-info "Unsupported decoded value" {:code :malformed}))))))

(defn- parse-value [format source]
  (portable
   (case format
     :json (decode-json source false)
     :jsonc (decode-json source true)
     :edn (decode-edn source)
     :yaml (decode-yaml source)
     :toml (let [result (Toml/parse ^String source)]
             (when (.hasErrors result) (throw (ex-info "Invalid TOML" {:code :malformed}))) result))))

(defn- parse-lines [source]
  (loop [lines (seq (str/split source #"(?<=\n)" -1)) offset 0 line 1 records []]
    (if-let [text (first lines)]
      (let [end (+ offset (alength (.getBytes ^String text "UTF-8")))
            span {:span/start-byte offset :span/end-byte end :span/start-line line
                  :span/end-line (inc line) :span/granularity :line}
            record (when-not (str/blank? text)
                     (try (let [decoded (parse-value :json text)]
                            {:record/status :parsed :record/line line :record/span span
                             :record/decoded decoded :record/value (value-tree decoded span 0)})
                          (catch Exception _ {:record/status :malformed :record/line line :record/span span})))]
        (recur (next lines) end (inc line) (cond-> records record (conj record))))
      records)))

(defn- chat-diagnostic [path span]
  {:diagnostic/code :malformed-conversation
   :diagnostic/source-path path :diagnostic/source-span span})

(defn- project-claim [result value span path]
  (if-not (conversations/declared-chat? value)
    result
    (try
      (let [errors (conversations/claim-errors value)
            projected (when (empty? errors) (conversations/project value span))]
        (if (and (seq projected) (every? #(m/validate law/conversation-schema %) projected))
          (update result :resource/conversations into
                  (map #(-> %
                            (update :conversation/source-path (fn [source] (into path source)))
                            (update :conversation/messages
                                    (fn [messages]
                                      (mapv (fn [message]
                                              (update message :message/source-path
                                                      (fn [source] (into path source)))) messages)))) projected))
          (update result :resource/diagnostics into
                  (map #(chat-diagnostic (into path %) span) (if (seq errors) errors [[]])))))
      (catch Exception _
        (update result :resource/diagnostics conj (chat-diagnostic path span))))))

(defn- with-conversations
  "Admit each claim independently; chat errors never discard successfully parsed data."
  [result value span]
  (reduce (fn [current [path claim]] (project-claim current claim span path))
          result (if (vector? value) (map-indexed (fn [index claim] [[index] claim]) value) [[[] value]])))

(defn- with-session-conversations [result records]
  (if-not (some #(= "session" (get-in % [:record/decoded "type"])) records)
    result
    (let [record-errors
          (into [] (keep (fn [record]
                           (let [event (:record/decoded record) line (:record/line record)]
                             (cond
                               (and (= "session" (get event "type")) (not (string? (get event "id"))))
                               (chat-diagnostic [line "id"] (:record/span record))
                               (and (= "message" (get event "type")) (not (map? (get event "message"))))
                               (chat-diagnostic [line "message"] (:record/span record)))))) records)
          projected (conversations/project-session records)
          message-errors (into [] (comp (mapcat :conversation/messages)
                                        (remove #(m/validate law/message-schema %))
                                        (map #(chat-diagnostic (:message/source-path %) (:message/source-span %)))) projected)
          admitted (mapv #(update % :conversation/messages
                                  (fn [messages] (filterv (fn [message] (m/validate law/message-schema message)) messages))) projected)]
      (-> result
          (assoc :resource/conversations admitted)
          (update :resource/diagnostics into (concat record-errors message-errors))))))

(defn parse
  "Return lossless bytes and typed source structure, or an explicit diagnostic.
   Spans are whole-document (JSONL: whole-line), never invented token spans."
  [path bytes]
  (let [format (format-for path)
        span {:span/start-byte 0 :span/end-byte (alength ^bytes bytes)
              :span/granularity :document}
        base {:resource/version 1 :resource/tier :derived :resource/format format
              :resource/parser (parser-version format)
              :resource/source-base64 (.encodeToString (Base64/getEncoder) bytes)
              :resource/source-sha256 (.encodeToString (Base64/getEncoder)
                                                       (.digest (MessageDigest/getInstance "SHA-256") bytes))
              :resource/source-span span :resource/conversations [] :resource/diagnostics []}]
    (cond
      (> (alength ^bytes bytes) max-source-bytes) (assoc base :resource/status :too-large)
      (= format :unsupported) (assoc base :resource/status :unsupported)
      :else
      (try
        (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                        (.onMalformedInput CodingErrorAction/REPORT)
                        (.onUnmappableCharacter CodingErrorAction/REPORT))
              source (str (.decode decoder (ByteBuffer/wrap bytes)))
              span (assoc span :span/start-line 1 :span/end-line (inc (count (str/split-lines source))))
              base (assoc base :resource/source-span span)]
          (if (= format :jsonl)
            (let [records (parse-lines source)]
              (with-session-conversations
                (assoc base :resource/status (if (every? #(= :parsed (:record/status %)) records) :parsed :malformed)
                       :resource/records (mapv #(dissoc % :record/decoded) records))
                records))
            (let [value (parse-value format source)]
              (with-conversations
                (assoc base :resource/status :parsed :resource/value (value-tree value span 0))
                value span))))
        (catch Exception _
          (assoc base :resource/status :malformed
                 :resource/diagnostics [{:diagnostic/code :malformed-resource :diagnostic/format format}]))
        (catch StackOverflowError _
          (assoc base :resource/status :malformed
                 :resource/diagnostics [{:diagnostic/code :parser-nesting-limit :diagnostic/format format}]))))))
