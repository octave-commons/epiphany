(ns epiphany.law.resource
  "Portable historical resource projections; parser output is derived, never accepted lineage.")

(def span-schema
  "Offsets are UTF-8 bytes, end-exclusive; granularity is explicitly disclosed."
  [:map {:closed true}
   [:span/start-byte [:int {:min 0}]] [:span/end-byte [:int {:min 0}]]
   [:span/start-line {:optional true} [:int {:min 1}]] [:span/end-line {:optional true} [:int {:min 1}]]
   [:span/granularity [:enum :document :line]]])

(def value-schema
  "A recursively typed portable value; source precision is carried by each span."
  [:schema
   {:registry
    {"resource/value"
     [:or
      [:map {:closed true} [:value/type [:= :null]] [:value/span span-schema]]
      [:map {:closed true} [:value/type [:= :boolean]] [:value/span span-schema] [:value/data :boolean]]
      [:map {:closed true}
       [:value/type [:enum :string :integer :float :decimal :ratio :keyword :symbol :character
                     :instant :uuid :LocalDate :LocalTime :LocalDateTime :OffsetDateTime]]
       [:value/span span-schema] [:value/data :string]]
      [:map {:closed true} [:value/type [:enum :vector :list :set]] [:value/span span-schema]
       [:value/items [:vector [:ref "resource/value"]]]]
      [:map {:closed true} [:value/type [:= :map]] [:value/span span-schema]
       [:value/entries [:vector [:map {:closed true}
                                 [:entry/key [:ref "resource/value"]]
                                 [:entry/value [:ref "resource/value"]]]]]]]}}
   [:ref "resource/value"]])

(def message-schema
  "Source IDs, role, speaker and timestamps are optional because absence is evidence."
  [:map {:closed true}
   [:message/source-path [:vector [:or :string :int]]]
   [:message/source-span span-schema]
   [:message/id {:optional true} [:maybe [:or :string 'integer?]]]
   [:message/role {:optional true} [:maybe :string]]
   [:message/speaker {:optional true} [:maybe :string]]
   [:message/timestamp {:optional true} [:maybe [:or :string 'number? 'inst?]]]
   [:message/order {:optional true} [:int {:min 0}]]
   [:message/source-node-id {:optional true} :string]
   [:message/parent-node-id {:optional true} [:maybe :string]]
   [:message/text {:optional true} [:maybe :string]]])

(def conversation-schema
  "Recognized conversations retain source array/line order or an explicit parent graph."
  [:map {:closed true}
   [:conversation/schema [:enum "epiphany.conversation.v1" "chatgpt.mapping.v1" "eta-mu.session.v1"]]
   [:conversation/order [:enum :array :source-lines :parent-graph]]
   [:conversation/messages [:vector message-schema]]
   [:conversation/id {:optional true} [:maybe [:or :string 'integer?]]]
   [:conversation/title {:optional true} [:maybe :string]]
   [:conversation/current-node {:optional true} :string]
   [:conversation/source-path {:optional true} [:vector [:or :string :int]]]
   [:conversation/source-span {:optional true} span-schema]])

(def representation-schema
  "Lossless source bytes plus typed values and recognized conversation projections."
  [:map {:closed true}
   [:resource/version [:= 1]]
   [:resource/tier [:= :derived]]
   [:resource/format :keyword]
   [:resource/parser :string]
   [:resource/status [:enum :parsed :malformed :unsupported :too-large]]
   [:resource/source-base64 :string]
   [:resource/source-sha256 :string]
   [:resource/source-span span-schema]
   [:resource/provenance {:optional true} :map]
   [:resource/value {:optional true}
    value-schema]
   [:resource/records {:optional true}
    [:vector [:map {:closed true}
              [:record/status [:enum :parsed :malformed]] [:record/line [:int {:min 1}]]
              [:record/span span-schema] [:record/value {:optional true} value-schema]]]]
   [:resource/conversations [:vector conversation-schema]]
   [:resource/diagnostics [:vector :map]]])
