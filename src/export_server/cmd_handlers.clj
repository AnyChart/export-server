(ns export-server.cmd-handlers
  (:require [export-server.utils.rasterizator :as rasterizator]
            [export-server.utils.phantom :as browser]
            [clojure.java.io :as io]))


(defn- get-pdf-size [options]
  (if (and
      (contains? options :pdf-width)
      (contains? options :pdf-height)
      (not (nil? (:pdf-width options)))
      (not (nil? (:pdf-height options)))) [(:pdf-width options) (:pdf-height options)] (:pdf-size options)))

(defn out [result options ext]
  (let [output-file (if (:output-file options) (:output-file options) (str (java.util.UUID/randomUUID)))
        output-path (:output-path options)
        file-name (if (.endsWith output-file ext) output-file (str output-file ext))]
    (if (not (.exists (io/file output-path))) (.mkdir (io/file output-path)))
    (if (nil? output-file)
      (println (rasterizator/to-base64 result))
        (with-open [w (clojure.java.io/output-stream (str output-path file-name))]
          (.write w result)))))


(defn png [options]
  (let [script (if (:script options) (:script options) (slurp (:input-file options)))
        svg ((browser/script-to-svg script true true options) :result)
        image (:result (rasterizator/svg-to-png svg (:image-width options) (:image-height options) (:force-transparent-white options)))]
    (out image options ".png")))


(defn jpg [options]
  (let [script (if (:script options) (:script options) (slurp (:input-file options)))
        svg ((browser/script-to-svg script true true options) :result)
        image (:result (rasterizator/svg-to-jpg svg (:image-width options) (:image-height options) (:force-transparent-white options) (:jpg-quality options)))]
    (out image options ".jpg")))


(defn pdf [options]
  (let [script (if (:script options) (:script options) (slurp (:input-file options)))
        svg ((browser/script-to-svg script true true options) :result)
        image (:result (rasterizator/svg-to-pdf svg (get-pdf-size options) (:pdf-landscape options) (:pdf-x options) (:pdf-y options)))]
    (out image options ".pdf")))


(defn svg [options]
  (let [script (if (:script options) (:script options) (slurp (:input-file options)))
        svg ((browser/script-to-svg script true true options) :result)
        output-file (:output-file options)]
    (if (nil? output-file)
      (println svg)
      (spit (if (.endsWith output-file ".svg") output-file (str output-file ".svg")) svg))))

