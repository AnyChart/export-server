(ns export-server.browser.selenium.script-to-png
  (:require [export-server.browser.selenium.common :as common]
            [export-server.browser.image-resizer :as image-resizer]
            [export-server.data.state :as state]
            [export-server.browser.templates :as html-templates]
            [export-server.data.config :as config]
            [clojure.java.io :as io])
  (:import (org.openqa.selenium Dimension By OutputType TakesScreenshot)
           (java.io File)))


;=======================================================================================================================
; Script --> PNG
;=======================================================================================================================
(defn- exec-script-to-png-via-file [d script exit-on-error options type]
  (let [prev-handles (.getWindowHandles d)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]
      (.window (.switchTo d) new-handle)
      (.setSize (.window (.manage d)) (Dimension. (:image-width options) (+ (:image-height options)
                                                                            (if (= :firefox (:engine @state/options)) 75 0))))
      (let [startup (try
                      (let [html (html-templates/create-script-html options script)
                            tmp-file (File/createTempFile "anychart-export-server" "")]
                        (spit tmp-file html)
                        (.get d (str "file://" (.getAbsolutePath tmp-file)))
                        (.delete tmp-file)
                        nil)
                      (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (seq (.findElements d (By/tagName "svg")))
                    (do
                      (Thread/sleep 10)
                      nil)
                    (if (> (System/currentTimeMillis) (+ now 5000))
                      nil
                      (do
                        (Thread/sleep 10)
                        (recur))))))
              (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))

            svg
            (try
              (common/get-svg d)
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (.getScreenshotAs (cast TakesScreenshot d) OutputType/BYTES)
            ;; we need to resize (on white background) cause FIREFOX crop height and it has white background
            screenshot (if (= :firefox (:engine @state/options))
                         (image-resizer/resize-image screenshot options)
                         screenshot)

            error (some #(when (not (nil? %)) %) [startup waiting])]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          (case type
            :png {:ok true :result screenshot}
            :svg {:ok true :result svg}))))))


(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))

(defn- exec-script-to-png [d script exit-on-error options type]
  ;(prn :image-size (:image-width options) (:image-height options))
  ;(prn :container-size (:container-width options) (:container-height options))
  ;(prn :result-container-size
  ;     (str (config/min-size (:image-width options) (:container-width options)))
  ;     (str (config/min-size (:image-height options) (:container-height options))))
  (let [prev-handles (.getWindowHandles d)
        prev-handle (first prev-handles)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (.window (.switchTo d) new-handle)
      (.setSize (.window (.manage d)) (Dimension. (:image-width @state/options) (+ (:image-height @state/options)
                                                                                   (if (= :firefox (:engine @state/options)) 75 0))))

      ;(prn "prev handles: " prev-handles)
      ;(prn "Current: " (.getWindowHandle (:webdriver d)))
      (let [startup
            (try
              (.executeScript d "document.getElementsByTagName(\"body\")[0].style.margin = 0;
                                 document.body.innerHTML = '<style>.anychart-credits{display:none;}</style><div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>'"
                              (into-array [(:container-id options)
                                           (str (config/min-size (:image-width options) (:container-width options)))
                                           (str (config/min-size (:image-height options) (:container-height options)))]))
              (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            binary
            (try
              (.executeScript d anychart-binary (into-array []))
              (catch Exception e (str "Failed to execute AnyChat Binary File\n" (.getMessage e))))

            script
            (try
              (.executeScript d script (into-array []))
              (catch Exception e (str "Failed to execute Script\n" (.getMessage e))))

            ;anychart.onDocumentReady doesn't work in firefox, so we need to retrigger it
            _ (when (= :firefox (:engine @state/options))
                (try
                  (.executeScript d "var evt = document.createEvent('Event');evt.initEvent('load', false, false);window.dispatchEvent(evt);" (into-array []))
                  (catch Exception _ nil)))

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (seq (.findElements d (By/tagName "svg")))
                    (do
                      (Thread/sleep 10)
                      nil)
                    (if (> (System/currentTimeMillis) (+ now 5000))
                      nil
                      (do
                        (Thread/sleep 10)
                        (recur))))))
              (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))

            ;resize
            ;(try
            ;  (.executeScript d replacesvgsize (into-array []) )
            ;  (catch Exception e (str "Failed to execute ReplaceSvgSize\n" (.getMessage e))))

            svg
            (try
              (common/get-svg d)
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (.getScreenshotAs d OutputType/BYTES)
            ;; we need to resize (on white background) cause FIREFOX crop height and it has white background
            screenshot (if (= :firefox (:engine @state/options))
                         (image-resizer/resize-image screenshot options)
                         screenshot)

            error (some #(when (not (nil? %)) %) [startup binary script waiting])]

        (.executeScript d "window.close(\"\")" (into-array []))

        (.window (.switchTo d) prev-handle)
        ;(prn "End handles: " (.getWindowHandles (:webdriver d)))
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))
        ;(prn "SVG: " svg)

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          (case type
            :png {:ok true :result screenshot}
            :svg {:ok true :result svg}))))))



(defn script-to-png [script quit-ph exit-on-error options type]
  (if-let [driver (if quit-ph (common/create-driver) (common/get-free-driver))]
    (let [svg (exec-script-to-png driver script exit-on-error options type)]
      (if quit-ph (.quit driver) (common/return-driver driver))
      svg)
    {:ok false :result "Driver isn't available\n"}))