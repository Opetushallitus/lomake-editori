(ns ataru.http.server
  (:require [taoensso.timbre :as log]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [nrepl.server :as nrepl]
            [aleph.http :as http]
            [aleph.flow :as flow]
            [com.stuartsierra.component :as component])
  (:import [java.util EnumSet]
           [io.aleph.dirigiste Stats$Metric]))

; When restarting, we want to keep the same repl running, otherwise our repl-session is lost
; and restarting the repl is meaningless
(def repl-started (atom false))

(defn start-repl! [repl-port]
  (when (and (:dev? env) (compare-and-set! repl-started false true))
    (do
      (nrepl/start-server :port repl-port :bind "0.0.0.0")
      (log/report "nREPL started on port" repl-port))))

(defrecord Server []
  component/Lifecycle

  (start [this]
    (let [server-setup (:server-setup this)
          port         (:port server-setup)
          repl-port    (:repl-port server-setup)
          handler      (cond-> (get-in this [:handler :routes])
                               (:dev? env) (wrap-reload))
          executor     (flow/utilization-executor 0.9 512)
          server       (http/start-server handler {:port port
                                                   :executor executor})]
      (start-repl! repl-port)
      (log/report (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (log/report "Stopping server")
    (try (.close (:server this)) (catch Exception e))
    (log/report "Stopped server")
    (assoc this :server nil)))

(defn new-server
  []
  (->Server))
