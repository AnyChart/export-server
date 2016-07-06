(ns export-server.state)

(def options (atom nil))

(defn set-options [opts]
  (reset! options opts))
