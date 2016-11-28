(ns export-server.sharing.twitter-utils
  (:require [oauth.one :as one]))

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