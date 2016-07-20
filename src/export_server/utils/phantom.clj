(ns export-server.utils.phantom
  (:import (org.openqa.selenium WebDriverException)
           (org.openqa.selenium.phantomjs PhantomJSDriver)
           (org.openqa.selenium.remote DesiredCapabilities))
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.taxi :refer :all]
            [clj-webdriver.driver :refer [init-driver]]
            [clojure.java.io :as io :refer [output-stream]]))

;====================================================================================
; PhantomJS initialization
;====================================================================================
(defn- create-driver []
  (init-driver {:webdriver (PhantomJSDriver. (DesiredCapabilities.))}))

(defonce drivers nil)

(defn- get-free-driver []
  (.poll drivers))

(defn- return-driver [driver]
  (.add drivers driver))

(defn setup-phantom []
  ;(set-driver! (init-driver {:webdriver (PhantomJSDriver. (DesiredCapabilities.))}))
  (alter-var-root (var drivers)
                  (fn [_]
                    (doto (java.util.concurrent.ConcurrentLinkedQueue.)
                      (.add (create-driver))
                      (.add (create-driver))
                      (.add (create-driver))
                      (.add (create-driver))))))

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

(defn- get-script [script]
  (str "document.body.innerHTML = '<div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>';"
       anychart-binary
       script))

(defn- exec-script-to-svg [d script exit-on-error options]
  (let [;run-all
        ; (try
        ;   (execute-script d (get-script script) [(:container-id options) (:container-width options) (:container-height options)])
        ;   (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))
        startup
        (try
          (execute-script d "document.body.innerHTML = '<div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>'", [(:container-id options) (:container-width options) (:container-height options)])
          (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))
        binary
        (try
          (execute-script d anychart-binary)
          (catch Exception e (str "Failed to execute AnyChat Binary File\n" (.getMessage e))))
        script
        (try
          (execute-script d script)
          (catch Exception e (str "Failed to execute Script\n" (.getMessage e))))
        waiting
        (try
          (let [now (System/currentTimeMillis)]
            (loop []
              (if (not-empty (elements d "svg"))
                nil
                (if (> (System/currentTimeMillis) (+ now 2000))
                  "error"
                  (do
                    (prn "Waiting for svg...")
                    (Thread/sleep 10)
                    (recur))))))
          (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))
        resize
        (try
          (execute-script d replacesvgsize)
          (catch Exception e (str "Failed to execute ReplaceSvgSize\n" (.getMessage e))))
        svg
        (try
          (html d (first (elements d "svg")))
          (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))
        shoutdown
        (try
          (execute-script d "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}", [])
          (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))
        error (some #(when (not (nil? %)) %) [startup binary script shoutdown waiting resize])]
    (if error
      (if exit-on-error (exit 1 error) {:ok false :result error})
      {:ok true :result (trim-svg-string (clojure.string/replace svg #"\"" "'"))})))

(defn script-to-svg [script quit-ph exit-on-error options]
  (if-let [driver (if quit-ph (create-driver) (get-free-driver))]
    (let [svg (exec-script-to-svg driver script exit-on-error options)]
      (if quit-ph (quit driver) (return-driver driver))
      svg)
    {:ok false :result "Driver isn't available\n"}))