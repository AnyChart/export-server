(ns export-server.browser.templates
  (:require [clojure.java.io :as io]
            [export-server.data.config :as config]))


(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))


(defn create-svg-html [svg]
  (str "<html lang=\"en\">
        <head>
        <meta charset=\"UTF-8\">
        <style>
          body {
           margin: 0;
          }
        </style>
        </head>
        <body>"
       svg
       "</body></html>"))


(defn create-script-html [options script]
  (str "<html lang=\"en\">
        <head>
        <meta charset=\"UTF-8\">
        <script>"
       anychart-binary
       "</script>
       <style>
         body{margin:0;}
         .anychart-credits{display:none;}
       </style>
       </head>
       <body>"
       "<div id='" (:container-id options) "' style='width:"
       (str (config/min-size (:image-width options) (:container-width options)))
       ";height:"
       (str (config/min-size (:image-height options) (:container-height options)))
       ":'></div>"
       "<script>"
       script
       "</script>"
       "</body></html>"))
