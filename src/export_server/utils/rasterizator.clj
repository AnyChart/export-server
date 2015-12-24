(ns export-server.utils.rasterizator
  (:import (org.openqa.selenium WebDriverException)
           (org.openqa.selenium.phantomjs PhantomJSDriver)
           (org.openqa.selenium.remote DesiredCapabilities)
           (java.io ByteArrayOutputStream)
           (org.apache.batik.transcoder.image JPEGTranscoder)
           (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput)
           (org.apache.batik.transcoder TranscoderOutput)
           (org.apache.batik.transcoder SVGAbstractTranscoder)
           (java.lang Float)
           (java.io StringReader)
           )
  (:require [clojure.data.codec.base64 :as b64]
            [tikkba.transcoder :as transcoder]
            [clj-webdriver.core :as core])
  (:use clj-webdriver.taxi
        clj-pdf.core
        [clj-webdriver.driver :only [init-driver]]
        [clojure.java.io :as io]
        [clojure.java.io :only [output-stream]]))

;====================================================================================
; PhantomJS initialization
;====================================================================================
(defn setup-phantom []
  (set-driver! (init-driver {:webdriver (PhantomJSDriver. (DesiredCapabilities.))})))

(defn exit [status msg]
  (println msg)
  (System/exit status))


;====================================================================================
; Script --> SVG
;====================================================================================
(def anychart-load-script "var args = arguments;(function(d) {var js, id = 'anychart', ref = d.getElementsByTagName('head')[0];if (d.getElementById(id)) {return;}js = d.createElement('script');js.id = id;js.src = args[0];ref.appendChild(js);}(document));")
(def anychart-script-path (str (io/resource "js/anychart-bundle.min.js")))
(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))
(def replacesvgsize (slurp (io/resource "js/replacesvgsize.min.js")))

(defn- trim-svg-string [str]
  (let [left-trim-str (clojure.string/replace str #"^\"" "")
        right-trim-str (clojure.string/replace left-trim-str #"\"$" "")]
    right-trim-str))


(defn exec-script-to-svg [script exit-on-error options]
  (let [startup
        (try
          (execute-script "document.body.innerHTML = '<div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>'", [(:container-id options) (:container-width options) (:container-height options)])
          (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))
        binary
        (try
          (execute-script anychart-binary)
          (catch Exception e (str "Failed to execute AnyChat Binary File\n" (.getMessage e))))
        script
        (try
          (execute-script script)
          (execute-script replacesvgsize)
          (catch Exception e (str "Failed to execute Script\n" (.getMessage e))))
        svg
        (try
          (html (first (elements "svg")))
          (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

        shoutdown
        (try
          (execute-script "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}", [])
          (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))

        error (some #(when (not (nil? %)) %) [startup binary script shoutdown])]
    (if error
      (if exit-on-error (exit 1 error) {:ok false :result error})
      {:ok true :result (trim-svg-string (clojure.string/replace svg #"\"" "'"))})))


(defn script-to-svg [script quit-ph exit-on-error options]
  (if (not (bound? (var *driver*))) (setup-phantom))
  (let [svg (exec-script-to-svg script exit-on-error options)]
    (if quit-ph (quit))
    svg))


;====================================================================================
; SVG --> PDF
;====================================================================================
(defn svg-to-pdf [svg pdf-size landscape x y]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (pdf [{:size pdf-size :orientation (if landscape :landscape nil)} [:svg {:translate [x y]} (trim-svg-string svg)]] out)
      {:ok true :result (.toByteArray out)})
    (catch Exception e {:ok false :result (.getMessage e)})))


;====================================================================================
; SVG --> JPG
;====================================================================================
(defn svg-to-jpg [svg widht height force-transparent-white quality]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [string-reader (new StringReader svg)
            transcoder-input (new TranscoderInput string-reader)
            transcoder-output (new TranscoderOutput out)
            transcoder (new JPEGTranscoder)]

        (.addTranscodingHint transcoder JPEGTranscoder/KEY_QUALITY (float quality))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_WIDTH (float widht))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_HEIGHT (float height))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_FORCE_TRANSPARENT_WHITE (boolean force-transparent-white))
        (.transcode transcoder transcoder-input transcoder-output)
        {:ok true :result (.toByteArray out)}
        ))
    (catch Exception e {:ok false :result (.getMessage e)})))

;====================================================================================
; SVG --> PNG
;====================================================================================
(defn svg-to-png [svg widht height force-transparent-white]
  (try
    (with-open [out (new ByteArrayOutputStream)]
      (let [string-reader (new StringReader svg)
            transcoder-input (new TranscoderInput string-reader)
            transcoder-output (new TranscoderOutput out)
            transcoder (new PNGTranscoder)]

        (.addTranscodingHint transcoder JPEGTranscoder/KEY_WIDTH (float widht))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_HEIGHT (float height))
        (.addTranscodingHint transcoder JPEGTranscoder/KEY_FORCE_TRANSPARENT_WHITE (boolean force-transparent-white))
        (.transcode transcoder transcoder-input transcoder-output)
        {:ok true :result (.toByteArray out)}
        ))
    (catch Exception e {:ok false :result (.getMessage e)})))

;====================================================================================
; Base64 encode
;====================================================================================
(defn to-base64 [byte-array] (String. (b64/encode byte-array) "UTF-8"))
