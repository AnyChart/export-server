(ns export-server.sharing.twitter
  (:use org.httpkit.server
        compojure.core)
  (:require [ring.util.response :as rutils :refer [redirect response]]
            [compojure.route :as route :refer [not-found]]
            [clj-http.client :as client]
            [oauth.one :as one :refer []]
            [cheshire.core :refer :all]
            [taoensso.timbre :as timbre]
            [export-server.utils.responce :as resp]
            [export-server.sharing.storage :as storage]
            [export-server.sharing.twitter-utils :as twutils :refer [create-oauth-request timestamp]]))

(def consumer
  (one/make-consumer
    {:access-uri     "https://api.twitter.com/oauth/access_token"
     :authorize-uri  "https://api.twitter.com/oauth/authorize"
     :callback-uri   "http://localhost:2000/sharing/twitter_oauth1"
     ;:callback-uri   "http://export.anychart.stg/sharing/twitter_oauth"
     :key            "ffhhDbj6TYVWKtcBh6QyzUTmz"
     :request-uri    "https://api.twitter.com/oauth/request_token"
     :secret         "v7UWu1ChJMHG5xfyd51hACqdNIj3mUidfVCQY47mAffVtQJoQz"
     :signature-algo :hmac-sha1}))

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
      (resp/json-success {:status :ok})
      (resp/json-error "Update status error"))
    (resp/json-error "Upload image error")))


(defn auth-url []
  (try
    (let [token-request (one/request-token-request consumer)
          token-response (client/request token-request)
          data (ring.util.codec/form-decode (:body token-response))]
      (if-let [oauth-token (get data "oauth_token")]
        (let [auth-url (one/authorization-url consumer {"oauth_token" oauth-token})]
          (redirect auth-url))
        (resp/json-error "Get oauth request token error")))
    (catch Exception e
      (timbre/error "Get authorization url error" e)
      (resp/json-error "Get authorization url error"))))


(defn twitter-old [{session :session :as request} img-base64]
  (if-let [creds (-> session :db :twitter)]
    (let [oauth-token (:oauth-token creds)
          oauth-token-secret (:oauth-token-secret creds)]
      (update-status-with-img oauth-token oauth-token-secret
                              img-base64
                              "www.anychart.com #anychart"))
    (do
      (let [response (auth-url)]
        (assoc-in response [:session :local] {:img  img-base64
                                              :time (timestamp)})))))

(defn twitter-oauth-old [{session :session :as request}]
  (let [;; pass oauth data ;oauth-token (get (:params req) "oauth_token") and ;oauth-verifier (get (:params req) "oauth_verifier")
        token-request (one/access-token-request consumer (:params request))
        token-response (try (client/request token-request)
                            (catch Exception e {}))
        creds (ring.util.codec/form-decode (:body token-response))
        oauth-token (get creds "oauth_token")
        oauth-token-secret (get creds "oauth_token_secret")]
    (if (and oauth-token oauth-token-secret)
      (let [response (if-let [image (-> session :local :img)]
                       (update-status-with-img oauth-token oauth-token-secret image
                                               "www.anychart.com #anychart")
                       (resp/json-error "Image upload time expired"))]
        (-> response
            (assoc-in [:session :db :twitter] {:oauth-token        oauth-token
                                               :oauth-token-secret oauth-token-secret})
            (assoc-in [:session :local] nil)))
      (do
        (timbre/error "Get access token error")
        (resp/json-error "Get access token  url error")))))


(defn twitter [{session :session :as request} img-base64]
  (let [response (if (-> session :db :twitter)
                   (rutils/response (twutils/confirm-dialog img-base64))
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
                       (rutils/response (twutils/confirm-dialog image))
                       (resp/json-error "Image upload time expired"))]
        (-> response
            (assoc-in [:session :db :twitter] {:oauth-token        oauth-token
                                               :oauth-token-secret oauth-token-secret})
            (assoc-in [:session :local] nil)))
      (do
        (timbre/error "Get access token error")
        (resp/json-error "Get access token  url error")))))

(defn twitter-confirm [{session :session :as request}]
  (if-let [creds (-> session :db :twitter)]
    (let [oauth-token (:oauth-token creds)
          oauth-token-secret (:oauth-token-secret creds)
          message (get-in request [:params "message"])]
      (if-let [image (-> session :local :img)]
        (update-status-with-img oauth-token oauth-token-secret image message)
        (resp/json-error "Image upload time expired")))
    (resp/json-error "Session error, probably, expired")))

(defn dialog [req]
  (twutils/confirm-dialog nil))

