(ns epiphany.law.observation-write
  "Transient write admission results; persisted operation replay remains unchanged."
  (:require [malli.core :as m]))

(def schemas
  "Legacy providers acknowledge with nil; explicit providers distinguish admission."
  {"observation/write-result"
   [:maybe [:map {:closed true}
            [:observation/write-status [:enum :accepted :duplicate]]]]})

(def valid-result?
  "Validate the complete result before a consumer counts or indexes a write."
  (m/validator (get schemas "observation/write-result")))
