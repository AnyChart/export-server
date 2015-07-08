(ns export-server.utils.params-validator
  (:require [bouncer.validators :as v]
            [bouncer.core :as bouncer]
            [export-server.utils.dictionary :refer :all]))



(def ^{:private true} pixel-re #"(?i)^[0-9]+(px)?$")
(def ^{:private true} boolean-re #"(?i)^(true|false|0|1)$")
(def ^{:private true} ratio-re #"(?i)([0-9]+\.[0-9]+)")

(def ^{:private true} rasterization-data-fields-validations {"data" v/required})

(def ^{:private true} image-fields-validations
  {"width"                   [[v/matches pixel-re :message "Width must be a number or pixel string"]]
   "height"                  [[v/matches pixel-re :message "Height must be a number or pixel string"]]
   "forceTransparentWhite"   [[v/matches boolean-re  :message "Height must be a boolean or one of the numbers: 0, 1"]]
   "force-transparent-white" [[v/matches boolean-re :message "Height must be a boolean or one of the numbers: 0, 1"]]
   "quality"                 [[v/matches ratio-re :message "Quality must be in range: 0.1 - 1."]]
   })

(defn- available-pdf-size? [size]
  (if (nil? size)
    true
    (contains? available-pdf-sizes (keyword size)))
  )

(def ^{:private true} pdf-fields-validations
  {"pdfSize"   [[available-pdf-size? :message (str "pdfSize must be one of the values:" (clojure.string/join ", " (map name (keys available-pdf-sizes))))]]
   "pdf-size"  [[available-pdf-size? :message (str "pdfSize must be one of the values:" (clojure.string/join ", " (map name (keys available-pdf-sizes))))]]
   "x"         [[v/matches pixel-re :message "X must be a number or pixel string"]]
   "pdf-x"     [[v/matches pixel-re :message "X must be a number or pixel string"]]
   "y"         [[v/matches pixel-re :message "Y must be a number or pixel string"]]
   "pdf-y"     [[v/matches pixel-re :message "Y must be a number or pixel string"]]
   "landscape" [[v/matches boolean-re :message "Landscape must be a boolean or one of the numbers: 0, 1"]]
   })

(def ^{:private true} image-params-validations (merge rasterization-data-fields-validations image-fields-validations))
(def ^{:private true} pdf-params-validations (merge rasterization-data-fields-validations pdf-fields-validations))


(defn validate-image-params [params]
  (cond
    (not (or (contains? params "dataType") (contains? params "data-type"))) [[(str "data-type must be one of the values: " (clojure.string/join ", " (map name available-rasterization-data-types)))]]
    (not (or (contains? params "responseType") (contains? params "response-type"))) [[(str "response-type must be one of the values: " (clojure.string/join ", " (map name available-rasterization-response-types)))]]
    :else (bouncer/validate params image-params-validations)))


(defn validate-pdf-params [params]
  (cond
    (not (or (contains? params "dataType") (contains? params "data-type"))) [[(str "data-type must be one of the values: " (clojure.string/join ", " (map name available-rasterization-data-types)))]]
    (not (or (contains? params "responseType") (contains? params "response-type"))) [[(str "response-type must be one of the values: " (clojure.string/join ", " (map name available-rasterization-response-types)))]]
    :else (bouncer/validate params pdf-params-validations)))

(defn valid-result? [result] (nil? (get result 0)))

(defn get-error-message [result] (clojure.string/join "," (get result 0)))