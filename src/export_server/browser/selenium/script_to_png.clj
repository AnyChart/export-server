(ns export-server.browser.selenium.script-to-png
  (:require [export-server.browser.selenium.common :as common]
            [export-server.browser.image-resizer :as image-resizer]
            [export-server.data.state :as state]
            [export-server.browser.templates :as html-templates]
            [export-server.data.config :as config]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre])
  (:import (org.openqa.selenium Dimension By OutputType TakesScreenshot)
           (java.io File)))


;=======================================================================================================================
; Script --> PNG
;=======================================================================================================================
(defn exec-script-to-png-via-file [d script options type]
  (try
    (let [prev-handles (.getWindowHandles d)
          prev-handle (first prev-handles)
          _ (.executeScript d "window.open(\"\")" (into-array []))
          new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
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

            error (first (filter some? [startup waiting]))]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)

        (if error
          {:ok false :result error}
          {:ok     true
           :result (case type :png screenshot :svg svg)
           :png    screenshot
           :svg    svg})))
    (catch Exception e
      (timbre/error "Exec script to png error: " e)
      {:ok false :result (str "Exec script to png error: " e)})))


(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))


(defn exec-script-to-png [d script options type]
  (try
    (let [prev-handles (.getWindowHandles d)
          prev-handle (first prev-handles)
          _ (.executeScript d "window.open(\"\")" (into-array []))
          new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (.window (.switchTo d) new-handle)
      (.setSize (.window (.manage d)) (Dimension. (:image-width options) (+ (:image-height options)
                                                                            (if (= :firefox (:engine @state/options)) 75 0))))
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

            error (first (filter some? [startup binary script waiting]))]

        (.executeScript d "window.close(\"\")" (into-array []))

        (.window (.switchTo d) prev-handle)
        ;(prn "End handles: " (.getWindowHandles (:webdriver d)))
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))
        ;(prn "SVG: " svg)

        (if error
          {:ok false :result error}
          {:ok     true
           :result (case type :png screenshot :svg svg)
           :png    screenshot
           :svg    svg})))

    (catch Exception e
      (timbre/error "Exec script to png error: " e)
      {:ok false :result (str "Exec script to png error: " e)})))


(defn script-to-png [script quit-ph exit-on-error options type]
  (if-let [driver (if quit-ph (common/create-driver) (common/get-free-driver))]

    (let [result (exec-script-to-png driver script options type)]

      (when (and (false? (:ok result)) exit-on-error)
        (common/exit driver 1 (:result result)))

      (if quit-ph
        (.quit driver)
        (if (:ok result)
          (common/return-driver driver)
          (do
            (try (.quit driver)
                 (catch Exception e (timbre/error "Quit driver error: "e)))
            (common/return-driver (common/create-driver)))))

      result)

    {:ok false :result "Driver isn't available\n"}))