(ns export-server.web.logging
  (require [taoensso.timbre :as timbre :refer [info error]]))

(defn wrap-log-error [handler-fn result request type]
  (let [error-message (if (string? result) result (:message result))
        params (:form-params request)
        params-str (apply str (map #(str "\n" (name (% 0)) ": " (% 1)) (seq params)))]
    (info (condp = type
            :processing (str "Error occurred during processing " (:uri request))
            :bad_params (str "Bad request params with " (:uri request)))
          "\n\tError message:\n" error-message
          "\n\tRequest's parameters:" params-str "\n"))
  (handler-fn result))