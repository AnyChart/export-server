(ns export-server.sharing.twitter
  (:use org.httpkit.server
        compojure.core)
  (:require [ring.util.response :as rutils :refer [redirect response]]
            [compojure.route :as route :refer [not-found]]
            [clj-http.client :as client]
            [oauth.one :as one :refer []]
            [cheshire.core :refer :all]
            [taoensso.timbre :as timbre]
            [export-server.utils.responce :as resp :refer [json-error json-success]]
            [export-server.sharing.storage :as storage]
            [export-server.sharing.twitter-utils :as twutils :refer [create-oauth-request timestamp confirm-dialog
                                                                     success-dialog error-dialog]]))

(def consumer nil)

(defn init [mode]
  (let [callback-uri (cond
                       (System/getProperty "dev") "http://localhost:2000/sharing/twitter_oauth"
                       (= mode "prod") "http://export.anychart.com/sharing/twitter_oauth"
                       :else "http://export.anychart.stg/sharing/twitter_oauth")]
    (alter-var-root (var consumer)
                    (constantly (one/make-consumer
                                  {:access-uri     "https://api.twitter.com/oauth/access_token"
                                   :authorize-uri  "https://api.twitter.com/oauth/authorize"
                                   :callback-uri   callback-uri
                                   :key            "ffhhDbj6TYVWKtcBh6QyzUTmz"
                                   :request-uri    "https://api.twitter.com/oauth/request_token"
                                   :secret         "v7UWu1ChJMHG5xfyd51hACqdNIj3mUidfVCQY47mAffVtQJoQz"
                                   :signature-algo :hmac-sha1})))))


(defn img-to-base64 [path]
  (with-open [out (java.io.ByteArrayOutputStream.)
              in (clojure.java.io/input-stream path)]
    (clojure.java.io/copy in out)
    (String. (clojure.data.codec.base64/encode (.toByteArray out)) "UTF-8")))

(defn update-status [oauth-token oauth-token-secret text-message media-id]
  (try
    (let [status-request (create-oauth-request consumer oauth-token oauth-token-secret
                                               "https://api.twitter.com/1.1/statuses/update.json"
                                               {"status"    text-message
                                                "media_ids" media-id})
          status-response (client/request status-request)]
      status-response)
    (catch Exception e (timbre/error "Update status error" e) false)))


(defn upload-image [oauth-token oauth-token-secret img-base64]
  (try
    (let [upload-request (create-oauth-request consumer oauth-token oauth-token-secret
                                               "https://upload.twitter.com/1.1/media/upload.json"
                                               {"media" img-base64})
          upload-resp (client/request upload-request)
          media-id (:media_id_string (parse-string (:body upload-resp) true))]
      media-id)
    (catch Exception e (timbre/error "Update status error" e) false)))


(defn update-status-with-img [oauth-token oauth-token-secret img-base64 text-message]
  (if-let [media-id (upload-image oauth-token oauth-token-secret img-base64)]
    (if-let [_ (update-status oauth-token oauth-token-secret text-message media-id)]
      (success-dialog "Chart has been posted!")
      (error-dialog "Update status error!"))
    (error-dialog "Upload image error")))


(defn auth-url []
  (try
    (let [token-request (one/request-token-request consumer)
          token-response (client/request token-request)
          data (ring.util.codec/form-decode (:body token-response))]
      (if-let [oauth-token (get data "oauth_token")]
        (let [auth-url (one/authorization-url consumer {"oauth_token" oauth-token})]
          (redirect auth-url))
        (error-dialog "Get oauth request token error")))
    (catch Exception e
      (timbre/error "Get authorization url error" e)
      (error-dialog "Get authorization url error"))))


(defn twitter [{session :session :as request} img-base64]
  (let [response (if (-> session :db :twitter)
                   (confirm-dialog img-base64)
                   (auth-url))]
    (assoc-in response [:session :local] {:img  img-base64
                                          :time (timestamp)})))

(defn twitter-oauth [{session :session :as request}]
  (let [;; pass oauth data ;oauth-token (get (:params req) "oauth_token") and ;oauth-verifier (get (:params req) "oauth_verifier")
        token-request (one/access-token-request consumer (:params request))
        token-response (try (client/request token-request)
                            (catch Exception e {}))
        creds (ring.util.codec/form-decode (:body token-response))
        oauth-token (get creds "oauth_token")
        oauth-token-secret (get creds "oauth_token_secret")]
    (if (and oauth-token oauth-token-secret)
      (let [response (if-let [image (-> session :local :img)]
                       (confirm-dialog image)
                       (error-dialog "Image upload time expired"))]
        (-> response
            (assoc-in [:session :db :twitter] {:oauth-token        oauth-token
                                               :oauth-token-secret oauth-token-secret})
            (assoc-in [:session :local] nil)))
      (do
        (timbre/error "Get access token error")
        (error-dialog "Get access token  url error")))))

(defn twitter-confirm [{session :session :as request}]
  (if-let [creds (-> session :db :twitter)]
    (let [oauth-token (:oauth-token creds)
          oauth-token-secret (:oauth-token-secret creds)
          message (get-in request [:params "message"])]
      (if-let [image (-> session :local :img)]
        (update-status-with-img oauth-token oauth-token-secret image message)
        (error-dialog "Image upload time expired")))
    (error-dialog "Session error, probably, expired")))

(defn dialog [req]
  (twutils/confirm-dialog nil))

