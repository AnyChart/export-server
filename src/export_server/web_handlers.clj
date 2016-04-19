(ns export-server.web-handlers
  (:require [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [dk.ative.docjure.spreadsheet :as spreadheet]
            [clojure.data.csv :as csv-parser]
            [export-server.utils.responce :refer :all]
            [export-server.utils.rasterizator :as rastr]
            [export-server.utils.params-validator :as params-validator]
            [export-server.utils.config :as config]
            [export-server.utils.rasterizator :as rast])
  (:import (org.apache.commons.io.output ByteArrayOutputStream)))


;=======================================================================================================================
; Params to png/jpg/pdf
;=======================================================================================================================
(defn get-number-unit [map key] (Integer/parseInt (first (re-find #"([-+]?[0-9]+)" (map key)))))

(def allow-script-executing (atom true))

(defn get-boolean-unit [map key]
  (let [val (clojure.string/lower-case (map key))]
    (or (= val "true") (= val "1"))))

(defn get-pdf-size [params]
  (cond
    (and (contains? params "pdf-width") (contains? params "pdf-height")) [(get-number-unit params "pdf-width") (get-number-unit params "pdf-height")]
    (contains? params "pdf-size") (params "pdf-size")
    (contains? params "pdfSize") (params "pdfSize")
    :else (:pdf-size config/defaults)))

(defn get-pdf-x [params]
  (cond
    (contains? params "pdf-x") (get-number-unit params "pdf-x")
    (contains? params "x") (get-number-unit params "x")
    :else (:pdf-x config/defaults)))

(defn get-pdf-y [params]
  (cond
    (contains? params "pdf-x") (get-number-unit params "pdf-y")
    (contains? params "x") (get-number-unit params "y")
    :else (:pdf-y config/defaults)))

(defn get-force-transparent-white [params]
  (cond
    (contains? params "force-transparent-white") (params "force-transparent-white")
    (contains? params "forceTransparentWhite") (params "forceTransparentWhite")
    :else (:force-transparent-white config/defaults)))


(defn get-data-type [params]
  (cond
    (contains? params "data-type") (params "data-type")
    (contains? params "dataType") (params "dataType")
    :else nil))

(defn get-response-type [params]
  (cond
    (contains? params "response-type") (params "response-type")
    (contains? params "responseType") (params "responseType")
    :else nil))


(defn params-to-options [params]
  {
   :container-id     (if (contains? params "container-id") (params "container-id") (config/defaults :container-id))
   :container-width  (if (contains? params "container-width") (params "container-width") (config/defaults :container-width))
   :container-height (if (contains? params "container-height") (params "container-height") (config/defaults :container-height))})

(defn- to-png [params]
  (let [data (params "data")
        data-type (get-data-type params)
        width (if (contains? params "width") (get-number-unit params "width") (:image-width config/defaults))
        height (if (contains? params "height") (get-number-unit params "height") (:image-height config/defaults))
        force-transparent-white (get-force-transparent-white params)]
    (cond
      (and (= data-type "script") (not @allow-script-executing)) {:ok false :result "Script executing is not allowed"}
      (= data-type "svg") (rastr/svg-to-png data width height force-transparent-white)
      (= data-type "script") (let [to-svg-result (rastr/script-to-svg data false false (params-to-options params))]
                               (if (to-svg-result :ok)
                                 (rastr/svg-to-png (to-svg-result :result) width height force-transparent-white)
                                 to-svg-result))
      :else {:ok false :result "Unknown data type"})))


(defn- to-jpg [params]
  (let [data (params "data")
        data-type (get-data-type params)
        width (if (contains? params "width") (get-number-unit params "width") (:image-width config/defaults))
        height (if (contains? params "height") (get-number-unit params "height") (:image-height config/defaults))
        force-transparent-white (get-force-transparent-white params)
        quality (if (contains? params "quality") (read-string (params "quality")) (:jpg-quality config/defaults))]
    (cond
      (and (= data-type "script") (not @allow-script-executing)) {:ok false :result "Script executing is not allowed"}
      (= data-type "svg") (rastr/svg-to-jpg data width height force-transparent-white quality)
      (= data-type "script") (let [to-svg-result (rastr/script-to-svg data false false (params-to-options params))]
                               (if (to-svg-result :ok)
                                 (rastr/svg-to-jpg (to-svg-result :result) width height force-transparent-white quality)
                                 to-svg-result))
      :else {:ok false :result "Unknown data type"})))


(defn- to-pdf [params]
  (let [data (params "data")
        data-type (get-data-type params)
        pdf-size (get-pdf-size params)
        landscape (if (contains? params "landscape") (get-boolean-unit params "landscape") (:pdf-landscape config/defaults))
        x (get-pdf-x params)
        y (get-pdf-y params)]
    (cond
      (and (= data-type "script") (not @allow-script-executing)) {:ok false :result "Script executing is not allowed"}
      (= data-type "svg") (rastr/svg-to-pdf data pdf-size landscape x y)
      (= data-type "script") (let [to-svg-result (rastr/script-to-svg data false false (params-to-options params))]
                               (if (to-svg-result :ok)
                                 (rastr/svg-to-pdf (to-svg-result :result) pdf-size landscape x y)
                                 to-svg-result))
      :else {:ok false :result "Unknown data type"})))


(defn- to-svg [params]
  (let [data (params "data")
        data-type (get-data-type params)]
    (cond
      (and (= data-type "script") (not @allow-script-executing)) {:ok false :result "Script executing is not allowed"}
      (= data-type "svg") {:ok true :result data}
      (= data-type "script") (rastr/script-to-svg data false false (params-to-options params))
      :else {:ok false :result "Unknown data type"})))


(defn- get-file-name [params] (if (and (contains? params "file-name") (string? (params "file-name"))) (params "file-name") "anychart"))


;=======================================================================================================================
; Handlers
;=======================================================================================================================
(defn png [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-image-params params)]
    (if (params-validator/valid-result? validation-result)
      (let [to-png-result (to-png params)
            response-type (get-response-type params)]
        (if (to-png-result :ok)
          (if (= response-type "base64")
            (json-success (rast/to-base64 (to-png-result :result)))
            (file-success (to-png-result :result) (get-file-name params) ".png"))
          (json-error (to-png-result :result))))
      (json-error (params-validator/get-error-message validation-result)))))


(defn jpg [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-image-params params)]
    (if (params-validator/valid-result? validation-result)
      (let [to-jpg-result (to-jpg params)
            response-type (get-response-type params)]
        (if (to-jpg-result :ok)
          (if (= response-type "base64")
            (json-success (rast/to-base64 (to-jpg-result :result)))
            (file-success (to-jpg-result :result) (get-file-name params) ".jpg"))
          (json-error (to-jpg-result :result))))
      (json-error (params-validator/get-error-message validation-result)))))


(defn pdf [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-pdf-params params)]
    (if (params-validator/valid-result? validation-result)
      (let [to-pdf-result (to-pdf params)
            response-type (get-response-type params)]
        (if (to-pdf-result :ok)
          (if (= response-type "base64")
            (json-success {:result (rast/to-base64 (to-pdf-result :result))})
            (file-success (to-pdf-result :result) (get-file-name params) ".pdf"))
          (json-error (to-pdf-result :result)))
        )
      (json-error (params-validator/get-error-message validation-result)))))


(defn svg [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-image-params params)]
    (if (params-validator/valid-result? validation-result)
      (let [to-svg-result (to-svg params)
            response-type (get-response-type params)]
        (if (to-svg-result :ok)
          (if (= response-type "base64")
            (json-success (rast/to-base64 (.getBytes (to-svg-result :result))))
            (file-success (.getBytes (to-svg-result :result)) (get-file-name params) ".svg"))
          (json-error (to-svg-result :result)))
        )
      (json-error (params-validator/get-error-message validation-result)))))


(defn xml [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-save-data-params params)]
    (if (params-validator/valid-result? validation-result)
      (file-success (.getBytes (params "data")) (get-file-name params) ".xml")
      (json-error (params-validator/get-error-message validation-result)))))


(defn json [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-save-data-params params)]
    (if (params-validator/valid-result? validation-result)
      (file-success (.getBytes (params "data")) (get-file-name params) ".json")
      (json-error (params-validator/get-error-message validation-result)))))


(defn csv [request]
  (let [params (request :form-params)
        validation-result (params-validator/validate-save-data-params params)]
    (if (params-validator/valid-result? validation-result)
      (file-success (.getBytes (params "data")) (get-file-name params) ".csv")
      (json-error (params-validator/get-error-message validation-result)))))


(defn xlsx [request]
  (let [params (request :form-params)
        file-name (get-file-name params)
        validation-result (params-validator/validate-save-data-params params)]
    (if (params-validator/valid-result? validation-result)
      (let [csv (csv-parser/read-csv (params "data"))
            wb (spreadheet/create-workbook file-name csv)
            output (new ByteArrayOutputStream)]
        (spreadheet/save-workbook! output wb)
        (file-success (.toByteArray output) file-name ".xlsx"))
      (json-error (params-validator/get-error-message validation-result)))))

