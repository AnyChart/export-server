(ns export-server.utils.rasterizator
  (:import (org.apache.batik.transcoder.image JPEGTranscoder)
           (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput)
           (org.apache.batik.transcoder TranscoderOutput)
           (org.apache.batik.transcoder SVGAbstractTranscoder)
           (java.io StringReader)
           (java.io ByteArrayOutputStream ByteArrayInputStream)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (java.awt Color))
  (:require [clojure.data.codec.base64 :as b64]
            [tikkba.transcoder :as transcoder]
            [digest :as d]
            [clj-pdf.core :refer :all]
            [clojure.java.io :as io :refer [output-stream]]))

;=======================================================================================================================
; SVG string helpers
;=======================================================================================================================
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

(defn- trim-svg-string [str]
  (let [left-trim-str (clojure.string/replace str #"^\"" "")
        right-trim-str (clojure.string/replace left-trim-str #"\"$" "")]
    right-trim-str))

(defn- remove-cursor [svg]
  (clojure.string/replace svg #"cursor\s*:\s*[\w-]+\s*;?\s*" ""))

(defn clear-svg [svg]
  (-> svg remove-cursor remove-empty-img remove-opacity replace-rgba))

;=======================================================================================================================
; SVG --> PDF
;=======================================================================================================================
(defn svg-to-pdf [img
                  {size      :pdf-size
                   width     :pdf-width
                   height    :pdf-height
                   landscape :pdf-landscape
                   x         :pdf-x
                   y         :pdf-y}]
  (try
    (with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/1.png"))]
      (.write out img))
    (with-open [out (new ByteArrayOutputStream)]
      (pdf [{:size          (or size [(* 0.75 width) (* 0.75 height)])
             :orientation   (if landscape :landscape nil)
             :footer        {:page-numbers false}
             :left-margin   0
             :right-margin  0
             :top-margin    0
             :bottom-margin 0}
            ;[:svg {:translate [x y]} (clear-svg (trim-svg-string svg))]
            [:image {:translate [x y]
                     ;:scale     (when (coll? size) 75)
                     :width     (* 0.75 width)
                     ;:height    height
                     } img]]
           out)
      {:ok true :result (.toByteArray out)})
    (catch Exception e {:ok false :result (.getMessage e)})))

;=======================================================================================================================
; PNG --> JPG
;=======================================================================================================================
(defn png-to-jpg [png-bytes]
  (let [bais (ByteArrayInputStream. png-bytes)
        bufferedImage (ImageIO/read bais)
        baos (ByteArrayOutputStream.)
        newBufferedImage (BufferedImage. (.getWidth bufferedImage)
                                         (.getHeight bufferedImage)
                                         BufferedImage/TYPE_INT_RGB)]
    (.drawImage (.createGraphics newBufferedImage) bufferedImage 0 0 Color/WHITE nil)
    (ImageIO/write newBufferedImage "JPG" baos)
    {:ok true :result (.toByteArray baos)}))

;=======================================================================================================================
; SVG --> JPG
;=======================================================================================================================
(defn svg-to-jpg [svg widht height force-transparent-white quality]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [svg (clear-svg svg)
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

;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn svg-to-png [svg widht height force-transparent-white]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [svg (clear-svg svg)
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

;=======================================================================================================================
; Base64 encode
;=======================================================================================================================
(defn to-base64 [byte-array] (String. (b64/encode byte-array) "UTF-8"))

;=======================================================================================================================
; File's name for local saving
;=======================================================================================================================
(defn get-file-name-hash [file-name]
  (let [hash (d/md5 (str (System/currentTimeMillis) "_" (rand-int (Integer/MAX_VALUE))))]
    (str file-name "_" hash)))