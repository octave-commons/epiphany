(ns epiphany.infra.repository-metadata-file-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [epiphany.infra.repository-metadata-file :as metadata-file]))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory "epiphany-metadata-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest writes-minimal-metadata-beneath-common-git-directory
  (let [common-git-dir (temporary-directory)
        resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
        path (metadata-file/write! (.getPath common-git-dir) resource-id)]
    (is (= (str (io/file common-git-dir "corpus-archaeology" "repository.edn")) path))
    (is (= "{:resource-id #uuid \"7a6b0d26-1000-4000-8000-000000000001\"}"
           (slurp path)))))

(deftest writes-are-idempotent-for-the-same-resource-id
  (let [common-git-dir (temporary-directory)
        resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
        first-write (metadata-file/write! (.getPath common-git-dir) resource-id)
        second-write (metadata-file/write! (.getPath common-git-dir) resource-id)]
    (is (= first-write second-write))
    (is (= {:resource-id resource-id}
           (metadata-file/read! (.getPath common-git-dir))))))

(deftest preserves-an-existing-resource-id
  (let [common-git-dir (temporary-directory)
        original-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
        competing-id #uuid "8b6b0d26-1000-4000-8000-000000000002"]
    (metadata-file/write! (.getPath common-git-dir) original-id)
    (try
      (metadata-file/write! (.getPath common-git-dir) competing-id)
      (is false "Expected a conflicting resource ID to fail")
      (catch clojure.lang.ExceptionInfo error
        (is (= original-id (:existing-resource-id (ex-data error))))
        (is (= competing-id (:requested-resource-id (ex-data error))))))
    (is (= {:resource-id original-id}
           (metadata-file/read! (.getPath common-git-dir))))))

