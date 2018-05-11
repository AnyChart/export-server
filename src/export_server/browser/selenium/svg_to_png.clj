(ns export-server.browser.selenium.svg-to-png
  (:require [export-server.utils.util :as util]
            [export-server.browser.selenium.common :as common]
            [export-server.utils.rasterizator :as rasterizator]
            [export-server.browser.templates :as html-templates]
            [export-server.data.state :as state])
  (:import (org.openqa.selenium Point Dimension OutputType TakesScreenshot)))


;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn add-data-text-html-base64-prefix [s64]
  (str "data:text/html;base64," s64))


(defn add-data-text-html-prefix [s64]
  (str "data:text/html," s64))


(defn- exec-svg-to-png [d svg exit-on-error {image-width :image-width image-height :image-height :as options}]
  (let [prev-handles (.getWindowHandles d)
        prev-handle (first prev-handles)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (.window (.switchTo d) new-handle)
      (when (and image-width image-height)
        (.setPosition (.window (.manage d)) (Point. image-width image-height))
        (.setSize (.window (.manage d)) (Dimension. image-width (+
                                                                  (if (= :firefox (:engine @state/options)) 75 0)
                                                                  image-height))))
      (let [startup (try
                      (let [url-encoded-data (add-data-text-html-base64-prefix (util/str-to-b64 (html-templates/create-svg-html svg)))]
                        (.get d url-encoded-data))
                      (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            screenshot (.getScreenshotAs (cast TakesScreenshot d) OutputType/BYTES)

            shoutdown
            (try
              (.executeScript d "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}", (into-array []))
              (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))

            error (some #(when (not (nil? %)) %) [startup shoutdown])]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          {:ok true :result screenshot})))))


(defn svg-to-png [svg quit-ph exit-on-error options]
  (if-let [driver (if quit-ph (common/create-driver) (common/get-free-driver))]
    (let [svg (rasterizator/clear-svg svg)
          png-result (exec-svg-to-png driver svg exit-on-error options)]
      (if quit-ph (.quit driver) (common/return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))