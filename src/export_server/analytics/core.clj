(ns export-server.analytics.core
  (:require [clojure.java.jdbc :as clj-jdbc]
            [honeysql.core :as sql]
            [honeysql.format :as sql-fmt]
            [honeysql.helpers :refer :all]
            [clojurewerkz.urly.core :refer [url-like]]
            [selmer.parser :refer [render-file]]
            [cheshire.core :refer [generate-string]]
            [export-server.web.responce :as utils]
            [taoensso.timbre :as timbre]
            [clojure.data.codec.base64 :as b64]
            [export-server.db.core :as db :refer :all]
            [cheshire.core :as json])
  (:import (java.util Calendar)))


(defonce state (atom {}))


(defn create-db-spec [db port user password]
  {:classname   "com.mysql.cj.jdbc.Driver"
   :subprotocol "mysql"
   :subname     (str "//localhost:" port "/" db "?characterEncoding=UTF-8&serverTimezone=UTC")
   :user        user
   :password    password
   :stringtype  "unspecified"})


(defn init [db port user password]
  (reset! state {:conn (db/connection-pool (create-db-spec db port user password))}))


;;======================================================================================================================
;; DB Inserting
;;======================================================================================================================
(defn store-domain [domain-data]
  (if (some? (:name domain-data))
    (or (:id (one @state (-> (select :id)
                             (from :domains)
                             (where [:= :name (:name domain-data)]))))
        (:generated_key (first (insert! @state :domains domain-data))))))


(defn store-request [hit-data domain-id]
  (exec @state (-> (insert-into :hits)
                   (values [(assoc hit-data :domain_id domain-id)]))))


(defn trim-www [domain]
  (if (and (string? domain) (.startsWith domain "www."))
    (subs domain 4)
    domain))

;; TODO: disabled stat/analytics
(defn store [request svg type] nil)
(defn store-disabled [request svg type]
  (try
    (let [referer-str (get-in request [:headers "referer"])
          domain (when referer-str (trim-www (.getHost (url-like referer-str))))
          address (or (get-in request [:headers "x-real-ip"])
                      (:remote-addr request))
          domain-data {:name domain}
          hit-data {:ip       address
                    :full_url referer-str
                    :svg      svg
                    :type     (name type)}]
      (store-request hit-data (store-domain domain-data)))
    (catch Exception e
      (timbre/error "Store error: " (.getMessage e)))))


;;======================================================================================================================
;; DB Select
;;======================================================================================================================
(defn update-svg [data]
  (let [svg (:svg data)
        ;[_ width height] (re-find  #"width=\"(\d+)\" height=\"(\d+)\"" svg)
        ;new-width (* 500 (/ (read-string width) (float 500)))
        ;new-height (* (float (/ new-width (read-string width))) (read-string height))
        ;svg* (clojure.string/replace-first svg #"width=\"([\d\.]+)\" height=\"([\d\.]+)\""
        ;                                   (str "width=\"" new-width "\" height=\"" new-height "\"" " viewbox=\"0 0 $1 $2\""))
        svg** (String. (b64/encode (.getBytes svg)) "UTF-8")]
    (assoc data :svg svg**)))


(defn get-svgs [lim off domains-condition]
  (let [svgs (query @state (-> (select :*)
                               (from :hits)
                               (join
                                 [(-> (select :hits.id :domains.name)
                                      (from :hits)
                                      (left-join :domains [:= :hits.domain_id :domains.id])
                                      (where domains-condition)
                                      (order-by [:hits.time :desc])
                                      (limit lim)
                                      (offset off)) "ids"]
                                 [:= :hits.id :ids.id])))]
    (map update-svg svgs)))


(defn total-count [domains-condition]
  (:value (one @state (-> (select [:%count.* "value"])
                          (from :hits)
                          (left-join :domains [:= :hits.domain_id :domains.id])
                          (where domains-condition)))))


(defn domain-names [domains]
  (let [names {:api       "api.anychart.com"
               :docs      "docs.anychart.com"
               :pg        "playground.anychart.com"
               :site      ["www.anychart.com" "anychart.com"]
               :stg       ["playground.anychart.stg" "anychart.stg" "docs.anychart.stg" "demos.anychart.dev"]
               :localhost ["localhost" "127.0.0.1"]}
        res (vec (flatten (map #(get names %) domains)))]
    res))


(defn in-cond [domains]
  (let [domains* (dissoc domains :external)
        res (filter (fn [[k v]] v) domains*)
        res* (map first res)]
    (if (seq res*)
      [:in :name (domain-names res*)]
      false)))


(defn not-in-cond [domains]
  (let [domains* (dissoc domains :external)
        res (filter (fn [[k v]] (not v)) domains*)
        res* (map first res)]
    (if (seq res*)
      [:not [:in :name (domain-names res*)]]
      true)))


(defn domain-contidion-checkbox [domains]
  (if (:external domains)
    (not-in-cond domains)
    (in-cond domains)))


(defn domain-condition-name [domain]
  (if (seq domain)
    [:like :name (str "%" domain "%")]
    true))


(defn domain-contidion [domains domain]
  [:and
   (domain-contidion-checkbox domains)
   (domain-condition-name domain)
   (sql/raw "svg is not null")])


;;======================================================================================================================
;; Requests
;;======================================================================================================================
(defn web [request]
  (let [domain (or (get-in request [:params "domain"]) "")
        domain-condition (domain-contidion {:external true :api false :docs false :site false :localhost false :pg false :stg false} domain)
        svgs (get-svgs 15 0 domain-condition)
        total-count (total-count domain-condition)]
    (render-file "templates/stat.html"
                 {:domain    domain
                  :init-data (generate-string {:svgs   svgs
                                               :total  total-count
                                               :offset 0
                                               :count  (count svgs)})})))


(defn svgs [request]
  (try
    (let [limit (-> request :body :limit)
          offset (-> request :body :offset)
          domains (-> request :body :domains)
          domain (-> request :body :domain)
          domain-contidion (domain-contidion domains domain)
          svgs (get-svgs limit offset domain-contidion)
          total-count (total-count domain-contidion)]
      (utils/json-success {:svgs svgs :total total-count :offset offset :count (count svgs)}))
    (catch Exception e
      (timbre/error e)
      (utils/json-error (.getMessage e)))))


;;======================================================================================================================
;; Summary
;;======================================================================================================================
(defn get-domains-by-month [year]
  (let [res (query @state (-> (select [:%monthname.time "month"] [:%count.* "num"])
                              (from [(-> (select :domains.name [:%min.time "time"])
                                         (from :hits)
                                         (join :domains [:= :hits.domain_id :domains.id])
                                         (group :domains.name)
                                         (having [:= :%year.time year])) "t1"])
                              (group :%monthname.time)
                              (order-by (sql/call :field :month "January" "February" "March" "April" "May"
                                                  "June" "July" "August" "September" "October" "November" "December"))))]
    (map (fn [data] [(:month data) (:num data)]) res)))


(defn get-count-by-month [year]
  (let [res (query @state (-> (select [:%monthname.time "month"] [:%count.* "num"])
                              (from :hits)
                              (where [:= :%year.time year])
                              (group :%monthname.time)
                              (order-by (sql/call :field :month "January" "February" "March" "April" "May"
                                                  "June" "July" "August" "September" "October" "November" "December"))))]
    (map (fn [data] [(:month data) (:num data)]) res)))


(defn get-last-domains []
  (let [res (query @state (-> (select :name [:%min.time "time"] [:%count.* "num"])
                              (from :hits)
                              (join :domains [:= :hits.domain_id :domains.id])
                              (group :name)
                              (having [:> :%min.time (sql/raw "DATE_SUB(NOW(), INTERVAL 1 MONTH)")])
                              (order-by [:time :desc])))]
    res))


(defn get-types []
  (let [res (query @state (-> (select :type [:%count.* "num"])
                              (from :hits)
                              (where [:<> :type nil])
                              (group :type)
                              (order-by [:num :desc])))]
    (map (fn [data] [(:type data) (:num data)]) res)))


(defn get-domains-count-by-month [year]
  (let [res (query @state (-> (select [:%monthname.time "month"] (sql/raw "count(DISTINCT name) as domains"))
                              (from :hits)
                              (join :domains [:= :hits.domain_id :domains.id])
                              (where [:= :%year.time year])
                              (group :%monthname.time)
                              (order-by (sql/call :field :month "January" "February" "March" "April" "May"
                                                  "June" "July" "August" "September" "October" "November" "December"))))]
    (map (fn [data] [(:month data) (:domains data)]) res)))


(defn summary [request]
  (let [current-year (.get (Calendar/getInstance) Calendar/YEAR)
        year (try (Integer/parseInt (get-in request [:params "year"]))
                  (catch Exception e
                    current-year))]
    (render-file "templates/summary.selmer"
                 {:year        year
                  :years       (range 2016 (inc current-year))
                  :chart-data1 (json/generate-string (get-domains-by-month year))
                  :chart-data2 (json/generate-string (get-domains-count-by-month year))
                  :chart-data3 (json/generate-string (get-count-by-month year))
                  :pie-data    (json/generate-string (get-types))
                  :table-data  (json/generate-string (get-last-domains))})))