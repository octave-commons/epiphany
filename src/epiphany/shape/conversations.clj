(ns epiphany.shape.conversations
  "Recognized source schemas only. Embedded instructions remain inert message data."
  (:require [clojure.string :as str]))

(defn- supplied
  "Copy only fields actually supplied by the source; never synthesize role, ID or time."
  [target source fields]
  (reduce (fn [result [from to]]
            (if (contains? source from) (assoc result to (get source from)) result)) target fields))

(defn- text-parts [content]
  (cond
    (string? content) content
    (and (map? content) (vector? (get content "parts")))
    (str/join "\n" (filter string? (get content "parts")))
    (vector? content)
    (str/join "\n" (keep #(when (and (= "text" (get % "type")) (string? (get % "text")))
                            (get % "text")) content))
    :else nil))

(defn- node-errors [node-id node]
  (let [path ["mapping" node-id] message (get node "message")]
    (cond
      (not (and (map? node) (contains? node "parent"))) [path]
      (and (some? message) (not (map? message))) [(conj path "message")]
      (and (map? message) (some? (get message "author")) (not (map? (get message "author"))))
      [(into path ["message" "author"])]
      :else [])))

(defn claim-errors
  "Return source-relative malformed claim locations before projecting foreign shapes."
  [value]
  (cond
    (= "epiphany.conversation.v1" (get value "schema"))
    (if (vector? (get value "messages"))
      (into [] (keep (fn [[index message]] (when-not (map? message) ["messages" index])))
            (map-indexed vector (get value "messages")))
      [[]])
    (and (map? value) (contains? value "mapping") (contains? value "current_node"))
    (if (and (map? (get value "mapping")) (string? (get value "current_node")))
      (into [] (mapcat (fn [[node-id node]] (node-errors node-id node))) (get value "mapping"))
      [[]])
    :else []))

(defn- chatgpt [value span]
  (when (and (map? value) (map? (get value "mapping"))
             (string? (get value "current_node"))
             (empty? (claim-errors value)))
    [(supplied
      {:conversation/schema "chatgpt.mapping.v1" :conversation/order :parent-graph
       :conversation/source-span span
       :conversation/current-node (get value "current_node")
       :conversation/messages
       (into [] (keep (fn [[node-id node]]
                        (when-let [message (get node "message")]
                          (cond-> (supplied {:message/source-node-id node-id
                                             :message/parent-node-id (get node "parent")
                                             :message/source-path ["mapping" node-id "message"]
                                             :message/source-span span}
                                            message [["id" :message/id] ["create_time" :message/timestamp]])
                            (contains? (get message "author") "role")
                            (assoc :message/role (get-in message ["author" "role"]))
                            (contains? (get message "author") "name")
                            (assoc :message/speaker (get-in message ["author" "name"]))
                            (some? (text-parts (get message "content")))
                            (assoc :message/text (text-parts (get message "content")))))))
             (get value "mapping"))}
      value [["id" :conversation/id] ["title" :conversation/title]])]))

(defn- declared-conversation [value span]
  (when (and (= "epiphany.conversation.v1" (get value "schema"))
             (vector? (get value "messages")) (every? map? (get value "messages")))
    [(supplied {:conversation/schema "epiphany.conversation.v1" :conversation/order :array
                :conversation/source-span span
                :conversation/messages
                (mapv (fn [index message]
                        (supplied {:message/order index :message/source-path ["messages" index]
                                   :message/source-span span}
                                  message [["id" :message/id] ["role" :message/role]
                                           ["speaker" :message/speaker] ["timestamp" :message/timestamp]
                                           ["text" :message/text]]))
                      (range) (get value "messages"))}
               value [["id" :conversation/id] ["title" :conversation/title]])]))

(defn project
  "Project known conversation schemas; a generic JSON object is never automatically chat."
  [value span]
  (if (vector? value)
    (into [] (mapcat (fn [[index item]]
                       (map #(-> %
                                 (assoc :conversation/source-path [index])
                                 (update :conversation/messages
                                         (fn [messages]
                                           (mapv (fn [message]
                                                   (update message :message/source-path
                                                           (fn [path] (into [index] path)))) messages))))
                            (or (declared-conversation item span) (chatgpt item span) []))))
          (map-indexed vector value))
    (or (declared-conversation value span) (chatgpt value span) [])))

(defn declared-chat?
  "Identify a claimed known schema even when it is malformed, so refusal is explicit."
  [value]
  (if (vector? value)
    (boolean (some declared-chat? value))
    (and (map? value)
         (or (= "epiphany.conversation.v1" (get value "schema"))
             (and (contains? value "mapping") (contains? value "current_node"))))))

(defn project-session
  "Recognize eta-mu session JSONL without truncation, invented times or fabricated authors."
  [records]
  (let [session (some #(when (= "session" (get-in % [:record/decoded "type"])) %) records)]
    (if (and session (string? (get-in session [:record/decoded "id"])))
      [{:conversation/schema "eta-mu.session.v1"
        :conversation/id (get-in session [:record/decoded "id"])
        :conversation/order :source-lines
        :conversation/messages
        (into [] (keep (fn [record]
                         (let [event (:record/decoded record) message (get event "message")]
                           (when (and (= "message" (get event "type")) (map? message))
                             (cond-> (supplied {:message/order (:record/line record)
                                                :message/source-path [(:record/line record) "message"]
                                                :message/source-span (:record/span record)}
                                               event [["id" :message/id] ["timestamp" :message/timestamp]])
                               (contains? message "role") (assoc :message/role (get message "role"))
                               (some? (text-parts (get message "content")))
                               (assoc :message/text (text-parts (get message "content")))))))) records)}]
      [])))
