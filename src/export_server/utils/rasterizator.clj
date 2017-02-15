(ns export-server.utils.rasterizator
  (:import (org.apache.batik.transcoder.image JPEGTranscoder)
           (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput)
           (org.apache.batik.transcoder TranscoderOutput)
           (org.apache.batik.transcoder SVGAbstractTranscoder)
           (java.io StringReader)
           (java.io ByteArrayOutputStream))
  (:require [clojure.data.codec.base64 :as b64]
            [tikkba.transcoder :as transcoder]
            [digest :as d]
            [clj-pdf.core :refer :all]
            [clojure.java.io :as io :refer [output-stream]]))

;====================================================================================
; SVG string helpers
;====================================================================================
;; remove empty images - cause of error during pdf processing
(defn- remove-empty-img [svg]
  (clojure.string/replace svg #"<image[^>]*xlink:href=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7\"[^\<]*" ""))

;; remove paths with 0.00001 (1e-005 for IE) opacity - cause of some other labels invisibility
(defn- remove-opacity [svg]
  (-> svg
      (clojure.string/replace #"<path[^>]*fill-opacity=\"0\.0+1\"[^<]*" "")
      (clojure.string/replace #"<path[^>]*fill-opacity=\"1e-005\"[^<]*" "")))

;; fill="rgba(0,232,121,0.05)" --> fill="rgba(0,232,121)" fill-opacity="0.05"
;; stroke="rgba(0,232,121,0.05)" --> stroke="rgba(0,232,121)" stroke-opacity="0.05"
(defn replace-rgba [svg]
  (-> svg
      (clojure.string/replace #"fill=\"rgba\(([^,\)]*),([^,\)]*),([^,\)]*),([^,\)]*)\)\""
                              "fill=\"rgb($1,$2,$3)\" fill-opacity=\"$4\"")
      (clojure.string/replace #"stroke=\"rgba\(([^,\)]*),([^,\)]*),([^,\)]*),([^,\)]*)\)\""
                              "stroke=\"rgb($1,$2,$3)\" stroke-opacity=\"$4\"")))

(defn- clear-svg [svg]
  (-> svg remove-empty-img remove-opacity replace-rgba))

(defn- trim-svg-string [str]
  (let [left-trim-str (clojure.string/replace str #"^\"" "")
        right-trim-str (clojure.string/replace left-trim-str #"\"$" "")]
    right-trim-str))

(defn- remove-cursor [svg]
  (clojure.string/replace svg #"cursor\s*:\s*[\w-]+\s*;?\s*" ""))

;====================================================================================
; SVG --> PDF
;====================================================================================
(defn svg-to-pdf [svg pdf-size landscape x y]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (pdf [{:size pdf-size :orientation (if landscape :landscape nil)}
            [:svg {:translate [x y]} (clear-svg (remove-cursor (trim-svg-string svg)))]]
           out)
      {:ok true :result (.toByteArray out)})
    (catch Exception e {:ok false :result (.getMessage e)})))


;====================================================================================
; SVG --> JPG
;====================================================================================
(defn svg-to-jpg [svg widht height force-transparent-white quality]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [svg (-> svg remove-cursor remove-empty-img replace-rgba)
            string-reader (new StringReader svg)
            transcoder-input (new TranscoderInput string-reader)
            transcoder-output (new TranscoderOutput out)
            transcoder (new JPEGTranscoder)]
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_QUALITY (float quality))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_WIDTH (float widht))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_HEIGHT (float height))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_FORCE_TRANSPARENT_WHITE (boolean force-transparent-white))
        (.transcode transcoder transcoder-input transcoder-output)
        {:ok true :result (.toByteArray out)}))
    (catch Exception e {:ok false :result (.getMessage e)})))

;====================================================================================
; SVG --> PNG
;====================================================================================
(defn svg-to-png [svg widht height force-transparent-white]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [svg (-> svg remove-cursor remove-empty-img replace-rgba)
            string-reader (new StringReader svg)
            transcoder-input (new TranscoderInput string-reader)
            transcoder-output (new TranscoderOutput out)
            transcoder (new PNGTranscoder)]
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_WIDTH (float widht))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_HEIGHT (float height))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_FORCE_TRANSPARENT_WHITE (boolean force-transparent-white))
        (.transcode transcoder transcoder-input transcoder-output)
        {:ok true :result (.toByteArray out)}))
    (catch Exception e {:ok false :result (.getMessage e)})))

;====================================================================================
; Base64 encode
;====================================================================================
(defn to-base64 [byte-array] (String. (b64/encode byte-array) "UTF-8"))

;====================================================================================
; File's name for local saving
;====================================================================================
(defn get-file-name-hash [file-name]
  (let [hash (d/md5 (str (System/currentTimeMillis) "_" (rand-int (Integer/MAX_VALUE))))]
    (str file-name "_" hash)))