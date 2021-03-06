(ns export-server.sharing.storage
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [export-server.db.core :as db]
            [export-server.sharing.twitter-utils :refer [timestamp]]
            [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [camel-snake-kebab.extras :refer [transform-keys]])
  (:import (java.util UUID)))

;CREATE TABLE auth (
;  id BIGINT NOT NULL AUTO_INCREMENT,
;  sn TINYINT NOT NULL,
;  user_id VARCHAR (1024),
;  screen_name VARCHAR (1024),
;  name VARCHAR (1024),
;  image_url VARCHAR (1024),
;  session CHAR (36) NOT NULL,
;  oauth_token VARCHAR(1024),
;  oauth_token_secret VARCHAR(1024),
;  time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
;  PRIMARY KEY (id)
;) DEFAULT CHARSET=utf8;

(defonce sn {:twitter   1
             :facebook  2
             :linkedin  3
             :pinterest 4})

;; local storage expired time: 1 day
(defonce ^:const expired (* 60 60 24))

;; for db connecion
(defonce state (atom {}))

;; local storage: for imgs
(defonce memory (atom {}))

(defn id->sn [id]
  (first (first (filter #(= id (second %)) sn))))

(defn sn->id [name]
  (get sn (keyword name)))

(defn create-db-spec [db port user password]
  {:classname   "com.mysql.cj.jdbc.Driver"
   :subprotocol "mysql"
   :subname     (str "//localhost:" port "/" db "?characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false")
   :user        user
   :password    password
   :stringtype  "unspecified"})

(defn init [db port user password]
  (reset! state {:conn (db/connection-pool (create-db-spec db port user password))}))

(defn read-db [key]
  (when (:conn @state)
    (let [auths (db/query @state (-> (h/select :sn :oauth-token :oauth-token-secret :image-url :screen-name :name :user-id)
                                     (h/from :auth) (h/where [:= key :session])))
          result (reduce #(assoc %1
                            (id->sn (:sn %2))
                            (transform-keys ->kebab-case (dissoc %2 :sn)))
                         {} auths)]
      ; (prn "Storage Read session: " key result)
      result)))

(defn delete-db [key]
  ; (prn "Storage Delete session: " key)
  (when (:conn @state)
    (db/exec @state (-> (h/delete-from :auth) (h/where [:= key :session])))))

(defn write-db [key data]
  (when (:conn @state)
    (delete-db key)
    ;(prn "Storage write session: " key data)
    (let [insert-rows (mapv (fn [[sn {:keys [oauth-token oauth-token-secret screen-name name user-id image-url]}]]
                              [key (sn->id sn) oauth-token oauth-token-secret user-id screen-name name image-url]) data)]
      (db/exec @state (-> (h/insert-into :auth)
                          (h/columns :session :sn :oauth_token :oauth_token_secret :user_id :screen_name :name :image_url)
                          (h/values insert-rows))))))

(defn read-local [key]
  (get @memory key))

(defn write-local [key data]
  (swap! memory assoc key data))

(defn delete-local [key]
  (swap! memory dissoc key))

(defn clear-old-local []
  (swap! memory (fn [memory]
                  (let [now (timestamp)
                        keys (->> memory
                                  (filter (fn [[_ {time :time}]] (> now (+ time expired))))
                                  (map first))]
                    (reduce #(dissoc %1 %2) memory keys)))))

;; session storage
(deftype DbStore []
  SessionStore
  (read-session [_ key]
    {:db    (read-db key)
     :local (read-local key)})

  (write-session [_ key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (clear-old-local)
      (when (:db data)
        (write-db key (:db data)))
      (when (:local data)
        (write-local key (:local data)))
      key))

  (delete-session [_ key]
    (delete-db key)
    (delete-local key)
    nil))

(defn create-storage []
  (DbStore.))
