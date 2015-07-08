(ns export-server.repl
  (:require [export-server.core :as core]
            [export-server.utils.dictionary :as dict]
            [org.httpkit.server]))

(def server-atom (atom nil))

(defn start-server []
  (swap! server-atom
         (fn [server-instance app port]
           (if (nil? server-instance)
             (do
               (prn (str "Start export-server on port: " port))
               (org.httpkit.server/run-server app {:port port}))
             (do
               (prn (str "error: export-server already started on port: " port))
               server-instance)))
         core/app
         (:port dict/defaults)))


(defn stop-server []
  (swap! server-atom
         (fn [server-instance]
           (if (nil? server-instance)
             (do
               (prn "error: export-server already stopped")
               server-instance)
             (do
               (prn "Stopping export-server")
               (server-instance)
               (prn "Stopped")
               nil)))))
