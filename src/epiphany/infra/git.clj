(ns epiphany.infra.git
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defn common-git-directory [repository-path]
  (let [{:keys [exit out err]} (shell/sh "git" "-C" repository-path "rev-parse" "--path-format=absolute" "--git-common-dir")]
    (if (zero? exit)
      (string/trim out)
      (throw (ex-info "Could not resolve Git common directory"
                      {:repository-path repository-path
                       :git-error (string/trim err)})))))
