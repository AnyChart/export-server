(ns export-server.utils.responce
  (:import (java.io File FileOutputStream))
  (:require [cheshire.core :refer :all])
  (:use ring.util.response
        [ring.util.time :only (format-date)]
        [ring.util.io :only (last-modified-date)]))

(defn json-success [result]
  (-> (response (generate-string (if (string? result) {:result result} result)))
      (status 200)
      (content-type "application/json")
      (header "Access-Control-Allow-Origin" "*")
      (header "Access-Control-Allow-Methods" "POST")
      (header "Access-Control-Allow-Headers" "X-Requested-With")))

(defn json-error [result]
  (-> (response (generate-string {:error (if (string? result) result (:message result))}))
      (status (or (:status result) 400))
      (content-type "application/json")
      (header "Access-Control-Allow-Origin" "*")
      (header "Access-Control-Allow-Methods" "POST")
      (header "Access-Control-Allow-Headers" "X-Requested-With")))

(defn file-success [byte-array file-name file-extention]
  (let [file  ((partial apply #(File/createTempFile %1 %2) ["anychart-export-tmp" file-extention]))
        fos (new FileOutputStream file)]
    (.write fos byte-array)
    (-> (response file)
        (status 200)
        (content-type (case file-extention
                        ".svg" "image/svg+xml"
                        ".pdf" "application/pdf"
                        ".png" "image/png"
                        ".xml" "application/xml"
                        ".json" "application/json"
                        ".csv" "text/csv"
                        ".xls" "application/vnd.ms-excel"
                        ".xlsx" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        ".jpg" "image/jpeg"))
        (header "Content-Length" (.length file))
        (header "Last-Modified" (format-date (last-modified-date file)))
        (header "Content-Description" "File Transfer")
        (header "Content-Disposition" (str "attachment; filename=\"" file-name file-extention "\""))
        (header "Content-Transfer-Encoding" "binary")
        (header "Pragma" "public")
        (header "Cache-Control" "must-revalidate, post-check=0, pre-check=0")
        (header "Access-Control-Allow-Origin" "*")
        (header "Access-Control-Allow-Methods" "POST")
        (header "Access-Control-Allow-Headers" "X-Requested-With"))))
