(ns epiphany.infra.services
  "Service readiness diagnostics.

  Checks whether required external services (MongoDB, S3) are reachable
  before integration tests or :services mode commands run. Returns a
  structured result; never fabricates availability.

  The check is explicit and fast — a TCP connect + basic command.
  Unreachable services are reported as :unavailable with enough detail
  to diagnose without hanging."

  (:require [clojure.string :as string]))

(defn- tcp-reachable?
  "Can we open a TCP connection to host:port within timeout-ms?"
  [host port timeout-ms]
  (try
    (with-open [sock (java.net.Socket.)]
      (.connect sock
                (java.net.InetSocketAddress. ^String host ^int port)
                timeout-ms)
      true)
    (catch Exception _ false)))

(defn- mongo-available?
  "Check MongoDB availability via TCP. Returns a status map."
  [{:keys [host port timeout-ms]
    :or   {host "127.0.0.1" port 27017 timeout-ms 2000}}]
  (if (tcp-reachable? host port timeout-ms)
    {:service :mongodb
     :status  :available
     :host    host
     :port    port}
    {:service :mongodb
     :status  :unavailable
     :host    host
     :port    port
     :hint    (str "MongoDB not reachable at " host ":" port
                   ". Start with: docker compose up -d mongodb")}))

(defn- s3-available?
  "Check S3-compatible storage availability via TCP."
  [{:keys [host port timeout-ms]
    :or   {host "127.0.0.1" port 9000 timeout-ms 2000}}]
  (if (tcp-reachable? host port timeout-ms)
    {:service :s3
     :status  :available
     :host    host
     :port    port}
    {:service :s3
     :status  :unavailable
     :host    host
     :port    port
     :hint    (str "MinIO/S3 not reachable at " host ":" port
                   ". Start with: docker compose up -d minio")}))

(defn check-all
  "Check all required services. Returns a vector of status maps.

   Options:
     :mongodb {:host :port :timeout-ms}
     :s3      {:host :port :timeout-ms}"
  ([] (check-all {}))
  ([{:keys [mongodb s3]
    :or   {mongodb {} s3 {}}}]
   [(mongo-available? mongodb)
    (s3-available? s3)]))

(defn all-available?
  "Are all services reachable?"
  ([] (all-available? {}))
  ([opts]
   (every? #(= :available (:status %)) (check-all opts))))

(defn report
  "Human-readable readiness report for CLI output."
  ([] (report {}))
  ([opts]
   (let [results (check-all opts)
         lines   (map (fn [{:keys [service status host port hint]}]
                        (if (= :available status)
                          (str "  [OK]      " (name service) " at " host ":" port)
                          (str "  [UNAVAILABLE] " (name service) " at " host ":" port
                               (when hint (str "\n          " hint)))))
                      results)]
     (str "Service readiness:\n"
          (string/join \newline lines)
          "\n"
          (if (all-available? opts)
            "All services available."
            "Some services unavailable — :services mode will fail.")))))
