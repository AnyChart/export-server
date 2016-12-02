(ns export-server.sharing.twitter-utils
  (:require [oauth.one :as one]
            [selmer.parser :refer [render-file]]
            [ring.util.response :as rutils :refer [response]]))

(defn timestamp []
  (quot (System/currentTimeMillis) 1000))

(defn nonce []
  (str "kYjzVBB8Y0ZFabxSW" (+ 10000 (rand-int 10000)) (timestamp)))

(defn create-oauth-request [consumer oautn-token oauth-token-secret url params]
  (let [request-data {:request-method :post
                      :url            url
                      :form-params    params
                      :oauth-headers  (sorted-map
                                        "oauth_consumer_key" (:key consumer)
                                        "oauth_nonce" (nonce)
                                        "oauth_signature_method" "HMAC-SHA1"
                                        "oauth_timestamp" (timestamp)
                                        "oauth_token" oautn-token
                                        "oauth_version" "1.0")}
        request (one/sign-request
                  consumer
                  request-data
                  oauth-token-secret)]
    (-> request (assoc-in [:headers "Accept"] "*/*"))))

(defn confirm-dialog [image]
  (response (render-file "templates/tw_dialog.selmer" {:image image})))

(defn success-dialog [message]
  (response (render-file "templates/tw_success.selmer" {:message message})))

(defn error-dialog [message]
  (response (render-file "templates/tw_error.selmer" {:message message})))