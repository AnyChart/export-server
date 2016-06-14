(ns export-server.utils.logging
  (require [taoensso.timbre :as timbre :refer [info error]]))

(defn wrap-log-error [handler-fn error request type]
  (let [params (:form-params request)
        params-str (apply str (map #(str "\n" (name (% 0)) ": " (% 1)) (seq params)))]
    (info (condp = type
            :processing (str "Error occurred during processing " (:uri request))
            :bad_params (str "Bad request params with " (:uri request)))
          "\n\tError message:\n" error
          "\n\tRequest's parameters:" params-str "\n"))
  (handler-fn error))