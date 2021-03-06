(ns export-server.handlers.cmd-handlers
  (:require [export-server.utils.rasterizator :as rasterizator]
            [export-server.browser.core :as browser]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (org.apache.commons.io FilenameUtils)
           (org.apache.commons.lang3 SystemUtils)))


;=======================================================================================================================
; Helpers
;=======================================================================================================================
(defn out [result options ext]
  (let [output-file (if (:output-file options) (:output-file options) (str (java.util.UUID/randomUUID)))
        output-path (:output-path options)
        file-name (if (.endsWith output-file ext) output-file (str output-file ext))]
    (if (not (.exists (io/file output-path))) (.mkdir (io/file output-path)))
    (if (nil? output-file)
      (println (rasterizator/to-base64 result))
      (with-open [w (clojure.java.io/output-stream (str output-path file-name))]
        (.write w result)))))


;=======================================================================================================================
; Handlers for script
;=======================================================================================================================
(defn script->png [options script]
  (let [image (:result (browser/script-to-png script true options :png))]
    (out image options ".png")))


(defn script->jpg [options script]
  (let [png (:result (browser/script-to-png script true options :png))
        image (:result (rasterizator/png-to-jpg png))]
    (out image options ".jpg")))


(defn script->pdf [options script]
  (let [png (:result (browser/script-to-png script true options :png))
        image (:result (rasterizator/svg-to-pdf png options))]
    (out image options ".pdf")))


(defn script->svg [options script]
  (let [svg (:result (browser/script-to-png script true options :svg))
        output-file (:output-file options)]
    (if (nil? output-file)
      (println svg)
      (spit (if (.endsWith output-file ".svg") output-file (str output-file ".svg")) svg))))


(defn script->export [options]
  (let [script (or (:script options)
                   (slurp (:input-file options)))]
    (case (:type options)
      "png" (script->png options script)
      "jpg" (script->jpg options script)
      "svg" (script->svg options script)
      "pdf" (script->pdf options script))))


;=======================================================================================================================
; SVG handlers
;=======================================================================================================================
(defn svg->png [options svg]
  (let [image (:result (browser/svg-to-png svg true options))]
    (out image options ".png")))


(defn svg->jpg [options svg]
  (let [image (:result (browser/svg-to-png svg true options))
        image (:result (rasterizator/png-to-jpg image))]
    (out image options ".jpg")))


(defn svg->svg [options svg]
  (out (.getBytes svg) options ".svg"))


(defn svg->pdf [options svg]
  (let [png (:result (browser/svg-to-png svg true options))
        image (:result (rasterizator/svg-to-pdf png options))]
    (out image options ".pdf")))


(defn svg->export [options]
  (let [svg (or (:svg options)
                (slurp (:svg-file options)))]
    (case (:type options)
      "png" (svg->png options svg)
      "jpg" (svg->jpg options svg)
      "svg" (svg->svg options svg)
      "pdf" (svg->pdf options svg))))


;=======================================================================================================================
; HTML page handlers
;=======================================================================================================================
(defn html->png [options file]
  (let [image (:result (browser/html-to-png file true options false))]
    (out image options ".png")))


(defn html->jpg [options file]
  (let [image (:result (browser/html-to-png file true options false))
        image (:result (rasterizator/png-to-jpg image))]
    (out image options ".jpg")))


(defn html->pdf [options file]
  (let [png (:result (browser/html-to-png file true options false))
        image (:result (rasterizator/svg-to-pdf png options))]
    (out image options ".pdf")))


(defn html->svg [options file]
  (let [svg (:result (browser/html-to-png file true options true))]
    (out (.getBytes svg) options ".svg")))


(defn html->export [options]
  (let [file (:html-file options)
        file (-> file io/file .getAbsolutePath FilenameUtils/normalize (string/replace #"\\" "/"))
        file (if SystemUtils/IS_OS_WINDOWS
               (str "/" file)
               file)
        file (str "file://" file)]
    (case (:type options)
      "png" (html->png options file)
      "jpg" (html->jpg options file)
      "svg" (html->svg options file)
      "pdf" (html->pdf options file))))