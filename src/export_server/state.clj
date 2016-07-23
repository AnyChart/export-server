(ns export-server.state)

(defonce server (atom nil))

(defn set-server [serv]
  (reset! server serv))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(def options (atom nil))

(defn set-options [opts]
  (reset! options opts))
