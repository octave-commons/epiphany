(ns epiphany.application.registration-test
  (:require [clojure.test :refer [deftest is]]
            [epiphany.application.registration :as registration]))

(def resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001")

(defn fake-ports [{:keys [existing-resource-id]}]
  (let [writes (atom [])
        observations (atom [])]
    {:ports {:git {:common-git-directory (fn [_] "/repos/notes/.git")}
             :repository-metadata {:read (fn [_] (when existing-resource-id {:resource-id existing-resource-id}))
                                   :write (fn [_ id] (swap! writes conj id))}
             :observations {:find-by-request-id (fn [_] nil)
                            :record-repository-location! (fn [observation]
                                                           (swap! observations conj observation))}}
     :writes writes
     :observations observations}))

(deftest registers-a-new-repository-with-a-new-resource-id
  (let [{:keys [ports writes observations]} (fake-ports {})
        result (registration/register! ports "/repos/notes")]
    (is (= "/repos/notes" (:repository-path result)))
    (is (= "/repos/notes/.git" (:common-git-dir result)))
    (is (uuid? (:resource-id result)))
    (is (= [(:resource-id result)] @writes))
    (is (= 1 (count @observations)))
    (let [obs (first @observations)]
      (is (= (:resource-id result) (:resource-id obs)))
      (is (= :repository/location-observed (:observation/type obs)))
      (is (= "/repos/notes" (get-in obs [:repository/path :path/raw])))
      (is (= "/repos/notes/.git" (get-in obs [:repository/common-git-dir :path/raw]))))))

(deftest registration-reuses-an-existing-git-local-resource-id
  (let [{:keys [ports writes observations]} (fake-ports {:existing-resource-id resource-id})
        result (registration/register! ports "/repos/notes")]
    (is (= resource-id (:resource-id result)))
    (is (empty? @writes))
    (is (= 1 (count @observations)))
    (let [obs (first @observations)]
      (is (= resource-id (:resource-id obs)))
      (is (= :repository/location-observed (:observation/type obs)))
      (is (= "/repos/notes" (get-in obs [:repository/path :path/raw])))
      (is (= "/repos/notes/.git" (get-in obs [:repository/common-git-dir :path/raw]))))))

(deftest git-resolution-failure-does-not-write-identity-or-observation
  (let [writes (atom [])
        observations (atom [])
        ports {:git {:common-git-directory (fn [_]
                                             (throw (ex-info "Not a Git repository"
                                                             {:repository-path "/not-a-repository"})))}
               :repository-metadata {:read (fn [_] (throw (ex-info "Should not read metadata" {})))
                                     :write (fn [_ id] (swap! writes conj id))}
               :observations {:find-by-request-id (fn [_] nil)
                              :record-repository-location! (fn [observation]
                                                             (swap! observations conj observation))}}]
    (is (thrown? clojure.lang.ExceptionInfo
                 (registration/register! ports "/not-a-repository")))
    (is (empty? @writes))
    (is (empty? @observations))))

(deftest registration-observation-is-idempotent-by-request-id
  (let [writes (atom [])
        observations (atom {})
        ports {:git {:common-git-directory (fn [_] "/repos/notes/.git")}
               :repository-metadata {:read (fn [_] nil)
                                     :write (fn [_ id] (swap! writes conj id))}
               :observations {:find-by-request-id (fn [request-id] (get @observations request-id))
                               :record-repository-location! (fn [observation]
                                                              (swap! observations assoc (:observation/request-id observation) observation))}}
         command {:request-id #uuid "9a6b0d26-1000-4000-8000-000000000009"
                  :repository-path "/repos/notes"}
         first-result (registration/register! ports command)
         second-result (registration/register! ports command)]
     (is (= first-result second-result))
     (is (= 1 (count @writes)))
     (is (= 1 (count @observations)))
     (is (= (:request-id command) (:observation/request-id (first (vals @observations)))))))
