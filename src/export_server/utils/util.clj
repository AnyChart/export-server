(ns export-server.utils.util
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.string :as string])
  (:import (java.net URLEncoder)))


(defn jar-location
  "if run locally returns clojure.jar path"
  [& [ns]]
  (-> (or ns (class *ns*)) .getProtectionDomain .getCodeSource .getLocation .getPath clojure.java.io/file .getParent))


(defn str-to-b64 [s]
  (String. (b64/encode (.getBytes s)) "UTF-8"))


(defn str-to-url-encoded [s]
  (string/replace (URLEncoder/encode s "UTF-8") #"\+" "%20"))