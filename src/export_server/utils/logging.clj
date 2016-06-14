(ns export-server.utils.logging
  (require [taoensso.timbre :as timbre :refer [info error]]))

(defn wrap-log-error [handler-fn error params type]
  (let [params-str (apply str (map #(str "\n" (name (% 0)) ": " (% 1)) (seq params)))]
    (info (condp = type
            :processing "Error occurred during processing."
            :bad_params "Bad request params.")
          "\n\tError message:\n" error
          "\n\tRequest's parameters:" params-str "\n"))
  (handler-fn error))