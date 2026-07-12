(ns epiphany.law.markdown
  "Malli contracts for typed Markdown block trees.

  A Markdown document is a sequence of typed blocks, each carrying exact
  source spans (byte offset, line range) so the original text is
  recoverable from the raw blob. Headings carry an explicit hierarchy
  level. Front matter is parsed separately from the body.")

(def schemas
  {"md/span"
   [:map {:closed true}
    [:span/start-byte :int]
    [:span/end-byte :int]
    [:span/start-line :int]
    [:span/end-line :int]]

   "md/front-matter"
   [:map {:closed true}
    [:block/type [:= :front-matter]]
    [:block/raw :string]
    [:block/span [:ref "md/span"]]]

   "md/heading"
   [:map {:closed true}
    [:block/type [:= :heading]]
    [:heading/level :int]
    [:heading/inlines [:vector :map]]
    [:block/span [:ref "md/span"]]]

   "md/paragraph"
   [:map {:closed true}
    [:block/type [:= :paragraph]]
    [:paragraph/inlines [:vector :map]]
    [:block/span [:ref "md/span"]]]

   "md/fenced-code"
   [:map {:closed true}
    [:block/type [:= :fenced-code]]
    [:code/info-string :string]
    [:code/literal :string]
    [:block/span [:ref "md/span"]]]

   "md/block-quote"
   [:map {:closed true}
    [:block/type [:= :block-quote]]
    [:block-children [:vector [:ref "md/block"]]]
    [:block/span [:ref "md/span"]]]

   "md/bullet-list"
   [:map {:closed true}
    [:block/type [:= :bullet-list]]
    [:list/loose? :boolean]
    [:list-items [:vector [:ref "md/list-item"]]]
    [:block/span [:ref "md/span"]]]

   "md/ordered-list"
   [:map {:closed true}
    [:block/type [:= :ordered-list]]
    [:list/loose? :boolean]
    [:list/start-number {:optional true} :int]
    [:list-items [:vector [:ref "md/list-item"]]]
    [:block/span [:ref "md/span"]]]

   "md/list-item"
   [:map {:closed true}
    [:block/type [:= :list-item]]
    [:item/number {:optional true} :int]
    [:block-children [:vector [:ref "md/block"]]]
    [:block/span [:ref "md/span"]]]

   "md/table"
   [:map {:closed true}
    [:block/type [:= :table]]
    [:table/header-rows [:vector [:vector [:vector :map]]]]
    [:table/body-rows [:vector [:vector [:vector :map]]]]
    [:block/span [:ref "md/span"]]]

   "md/thematic-break"
   [:map {:closed true}
    [:block/type [:= :thematic-break]]
    [:block/span [:ref "md/span"]]]

   "md/html-block"
   [:map {:closed true}
    [:block/type [:= :html-block]]
    [:html/literal :string]
    [:block/span [:ref "md/span"]]]

   "md/diagnostic"
   [:map {:closed true}
    [:block/type [:= :diagnostic]]
    [:diagnostic/unsupported-type :string]
    [:diagnostic/raw :string]
    [:block/span [:ref "md/span"]]]

   "md/block"
   [:multi {:dispatch :block/type}
    [:front-matter [:ref "md/front-matter"]]
    [:heading [:ref "md/heading"]]
    [:paragraph [:ref "md/paragraph"]]
    [:fenced-code [:ref "md/fenced-code"]]
    [:block-quote [:ref "md/block-quote"]]
    [:bullet-list [:ref "md/bullet-list"]]
    [:ordered-list [:ref "md/ordered-list"]]
    [:table [:ref "md/table"]]
    [:thematic-break [:ref "md/thematic-break"]]
    [:html-block [:ref "md/html-block"]]
    [:diagnostic [:ref "md/diagnostic"]]]

   "md/document"
   [:map {:closed true}
    [:doc/front-matter {:optional true} [:ref "md/front-matter"]]
    [:doc/body [:vector [:ref "md/block"]]]
    [:doc/source-length :int]]})
