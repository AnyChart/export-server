(ns export-server.sharing.facebook
  (:use [compojure.route :only [not-found]]
        org.httpkit.server
        compojure.core)
  (:require [ring.util.response :refer [redirect]]
            [compojure.route :as route]
            [org.httpkit.client :as client]
    ;;[clj-http.client :as client]
            ))

(def app-id "1977415532485286")

(defn oath [app-id redirect-uri]
  (str "https://www.facebook.com/v2.8/dialog/oauth?client_id=" app-id "&redirect_uri=" redirect-uri))

(defn login-page []
  )

(defn facebook-oauth [req]
  (prn (str "Oath: " req))
  "Hello"
  )

;window.open("http://localhost:2000/sharing/facebook",'name','height=200,width=150');
; localhost:2000/sharing/facebook
(defn facebook [req]
  (prn (str "Share: " req))

  ;(redirect (oath app-id "http://127.0.0.1/sharing_oauth"))
  (redirect (oath app-id "http://localhost:2000/sharing/facebook_oauth"))
  ;(redirect (oath app-id "http://export.anychart.com/sharing_oauth"))

  )
