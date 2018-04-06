(ns export-server.sharing.core
  (:require [export-server.sharing.storage :as storage]
            [export-server.sharing.twitter :as twitter]))

(defn init [options]
  (let [{:keys [sharing-port sharing-db sharing-user sharing-password
                twitter-key twitter-secret twitter-callback]} options]
    (when (and sharing-db sharing-port sharing-user sharing-password
               twitter-key twitter-secret twitter-callback)
      (storage/init (:sharing-db options)
                    (:sharing-port options)
                    (:sharing-user options)
                    (:sharing-password options))
      (twitter/init (:twitter-key options)
                    (:twitter-secret options)
                    (:twitter-callback options))
      true)))