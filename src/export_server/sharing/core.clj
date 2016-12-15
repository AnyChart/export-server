(ns export-server.sharing.core
  (:require [export-server.sharing.storage :as storage]
            [export-server.sharing.twitter :as twitter]))

(defn init [mode]
  (storage/init mode)
  (twitter/init mode))