(ns epiphany.infra.request-identity-review-test
  "Review regressions for explicit command identity and HTTP profile parity."
  (:require [clio.extern.jvm.fs :as fs]
            [clio.infra.runtime :as runtime]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [epiphany.application.candidate-seeding :as candidate-seeding]
            [epiphany.application.commands :as commands]
            [epiphany.application.registration :as registration]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.http :as http]
            [epiphany.infra.main :as main]
            [epiphany.infra.profile :as profile]
            [epiphany.law-suite.observations-laws :as laws])
  (:import [org.eclipse.jgit.api Git]))

(defn- captured-error [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo error (ex-data error))))

(defn- recording-ports [calls]
  {:git {:common-git-directory (fn [_] (swap! calls conj :git) "/repo/.git")}
   :repository-metadata {:read (fn [_] (swap! calls conj :read) nil)
                         :write (fn [& _] (swap! calls conj :metadata))}
   :observations {:find-by-request-id (fn [_] (swap! calls conj :lookup) nil)
                  :record-repository-location! (fn [_] (swap! calls conj :record))}})

(deftest http-profile-spellings-select-the-configured-observation-provider
  (let [writes (atom [])
        marked (fn [selected]
                 (let [ports (in-memory/make {:common-git-dir-fn (constantly "/repo/.git")})
                       write (get-in ports [:observations :record-repository-location!])]
                   (assoc-in ports [:observations :record-repository-location!]
                             (fn [record] (swap! writes conj selected) (write record)))))
        app (http/create-handler (marked :local)
                                 {:default-profile :local
                                  :profile-adapters {:edn (marked :edn)
                                                     :services (marked :services)}})]
    (doseq [selected [:local :edn :services]
            spelling [(name selected) (str selected)]
            channel [:header :query]]
      (let [request {:request-method :post :uri "/api/v1/register"
                     :headers {} :body-params {:path "/repo" :request-id (random-uuid)}}
            request (case channel
                      :header (assoc-in request [:headers "x-profile"] spelling)
                      :query (assoc request :query-string
                                    (str "profile=" (string/replace spelling ":" "%3A"))))]
        (is (= 201 (:status (app request))) (pr-str [selected spelling channel]))
        (is (= selected (last @writes)))))
    (is (= 12 (count @writes)))
    (is (= 400 (:status (app {:request-method :get :uri "/api/v1/unknown"
                              :headers {"x-profile" "::edn"}}))))))

(deftest registration-law-requires-a-caller-owned-uuid
  (let [command {:command/name :command/register :repository-path "/repo"}]
    (doseq [candidate [command (assoc command :request-id nil)
                       (assoc command :request-id "not-a-uuid")]]
      (is (commands/rejected? (commands/decode candidate))))
    (let [identified (assoc command :request-id (random-uuid))]
      (is (= identified (commands/decode identified))))))

(deftest direct-registration-rejects-invalid-identity-before-any-port-call
  (doseq [command ["/repo" nil {:repository-path "/repo"}
                   {:repository-path "/repo" :request-id nil}
                   {:repository-path "/repo" :request-id "not-a-uuid"}]]
    (let [calls (atom [])]
      (is (= :bad-request
             (:code (captured-error #(registration/register! (recording-ports calls) command)))))
      (is (empty? @calls)))))

(deftest cli-and-http-refuse-missing-registration-identity-before-effects
  (let [calls (atom [])
        ports (recording-ports calls)
        handler (http/create-handler ports)]
    (with-redefs [profile/resolve-adapters (fn [_] (swap! calls conj :resolve) ports)]
      (let [result (main/run ["register" "--profile" "local" "/repo"])]
        (is (= 1 (:exit result)))
        (is (string/includes? (:out result) "request-id"))))
    (is (= 400 (:status (handler {:request-method :post :uri "/api/v1/register"
                                  :headers {} :body-params {:path "/repo"}}))))
    (is (empty? @calls))))

(defn- with-repository [run]
  (let [directory (str (System/getProperty "java.io.tmpdir")
                       "/epiphany-command-review-" (random-uuid))
        repository (str directory "/repo")]
    (try
      (fs/ensure-dir! repository)
      (fs/write-text! (str repository "/notes.md") "# Notes\n\nA recorded idea.\n")
      (with-open [git (.call (.setDirectory (Git/init) (java.io.File. repository)))]
        (.call (.addFilepattern (.add git) "notes.md"))
        (.call (doto (.commit git)
                 (.setAuthor "Review Test" "review@example.test")
                 (.setCommitter "Review Test" "review@example.test")
                 (.setMessage "Record a review fixture"))))
      (run repository (str directory "/ledger"))
      (finally (fs/remove-tree! directory)))))

(deftest candidate-retries-return-the-original-durable-identity
  (with-repository
    (fn [repository ledger]
      (let [request-id (random-uuid)
            args ["diff" "--repo" repository "--profile" "edn"
                  "--request-id" (str request-id) "--seed-candidate" "continues"
                  "notes.md@HEAD" "notes.md@HEAD"]]
        (with-redefs [profile/resolve-adapters
                      (fn [_] {:observations (clio/make-observations-adapter (clio/open-store ledger))})]
          (let [first-result (main/run args)
                before (clio/history (clio/open-store ledger))
                retry (main/run args)
                stored (get-in (first before) [:event/data :arguments 0])]
            (is (zero? (:exit first-result)) (:out first-result))
            (is (= 1 (count before)))
            (is (= request-id (:observation/request-id stored)))
            (is (= first-result retry))
            (is (= before (clio/history (clio/open-store ledger))))
            (let [conflict (main/run (mapv #(if (= % "continues") "refines" %) args))]
              (is (= 1 (:exit conflict)))
              (is (string/includes? (:out conflict) "request-id"))
              (is (= before (clio/history (clio/open-store ledger)))))
            (when stored
              (is (string/includes? (:out retry) (str (:lineage-candidate/id stored)))))))))))

(deftest candidate-seeding-requires-identity-before-repository-access
  (let [result (main/run ["diff" "--repo" "/no-such-review-repository"
                          "--seed-candidate" "continues" "notes.md@HEAD" "notes.md@HEAD"])]
    (is (= 1 (:exit result)))
    (is (string/includes? (:out result) "request-id"))))

(deftest candidate-retry-reforces-the-existing-ledger-before-success
  (let [directory (str (System/getProperty "java.io.tmpdir")
                       "/epiphany-candidate-fence-" (random-uuid))
        observation ((get-in laws/op-fixtures [:record-lineage-candidate! :make-valid]) (random-uuid))]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            _ ((:record-lineage-candidate! port) observation)
            before (clio/history store)
            forces (atom 0)]
        (with-redefs [runtime/ensure-durable!
                      (fn [& _]
                        (swap! forces inc)
                        (throw (ex-info "Injected durability refusal" {:code :unavailable})))]
          (is (= :unavailable (:code (captured-error #(candidate-seeding/record! port observation)))))
          (is (= 1 @forces)))
        (is (= before (clio/history store))))
      (finally (fs/remove-tree! directory)))))

(deftest registration-retries-refuse-changed-path-and-reforce-durability
  (let [directory (str (System/getProperty "java.io.tmpdir")
                       "/epiphany-registration-fence-" (random-uuid))
        observation ((get-in laws/op-fixtures [:record-repository-location! :make-valid]) (random-uuid))]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            _ ((:record-repository-location! port) observation)
            before (clio/history store)
            calls (atom [])
            ports (assoc (recording-ports calls) :observations port)
            command {:repository-path (get-in observation [:repository/path :path/raw])
                     :request-id (:observation/request-id observation)}
            forces (atom 0)]
        (is (= :bad-request
               (:code (captured-error #(registration/register!
                                        ports (update command :repository-path str "/different"))))))
        (with-redefs [runtime/ensure-durable!
                      (fn [& _]
                        (swap! forces inc)
                        (throw (ex-info "Injected durability refusal" {:code :unavailable})))]
          (is (= :unavailable (:code (captured-error #(registration/register! ports command)))))
          (is (= 1 @forces)))
        (is (empty? @calls))
        (is (= before (clio/history store))))
      (finally (fs/remove-tree! directory)))))
