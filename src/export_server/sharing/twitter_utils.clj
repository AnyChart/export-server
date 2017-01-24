(ns export-server.sharing.twitter-utils
  (:require [oauth.one :as one]
            [selmer.parser :refer [render-file]]
            [ring.util.response :as rutils :refer [response]]))

(defn timestamp []
  (quot (System/currentTimeMillis) 1000))

(defn users-show-request [consumer access-token access-token-secret screen_name]
  (one/sign-request consumer
                    {:request-method :get
                     :url            "https://api.twitter.com/1.1/users/show.json"
                     :query-params   {"screen_name" screen_name}}
                    {:token  access-token
                     :secret access-token-secret}))

(defn statuses-update-request [consumer access-token access-token-secret message media-id]
  (one/sign-request consumer
                    {:request-method :post
                     :url            "https://api.twitter.com/1.1/statuses/update.json"
                     :form-params    {"status"    message
                                      "media_ids" media-id}}
                    {:token  access-token
                     :secret access-token-secret}))

(defn media-upload-request [consumer access-token access-token-secret img-base64]
  (one/sign-request consumer
                    {:request-method :post
                     :url            "https://upload.twitter.com/1.1/media/upload.json"
                     :form-params    {"media" img-base64}}
                    {:token  access-token
                     :secret access-token-secret}))

(defn confirm-dialog [image profile-image-url screen-name name]
  (response (render-file "templates/tw_dialog.selmer" {:image image
                                                       :profile-image-url profile-image-url
                                                       :screen-name screen-name
                                                       :name name})))

(defn success-dialog [message]
  (response (render-file "templates/tw_success.selmer" {:message message})))

(defn error-dialog [message]
  (response (render-file "templates/tw_error.selmer" {:message message})))