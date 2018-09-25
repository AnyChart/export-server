(ns export-server.browser.selenium.svg-to-png
  (:require [export-server.utils.util :as util]
            [export-server.browser.selenium.common :as common]
            [export-server.utils.rasterizator :as rasterizator]
            [export-server.browser.templates :as html-templates]
            [export-server.data.state :as state]
            [taoensso.timbre :as timbre])
  (:import (org.openqa.selenium Point Dimension OutputType TakesScreenshot)))


;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn add-data-text-html-base64-prefix [s64]
  (str "data:text/html;charset=UTF-8;base64," s64))


(defn add-data-text-html-prefix [s64]
  (str "data:text/html," s64))


(defn exec-svg-to-png [d svg {image-width :image-width image-height :image-height :as options}]
  (try
    (let [prev-handles (.getWindowHandles d)
          prev-handle (first prev-handles)
          _ (.executeScript d "window.open(\"\")" (into-array []))
          new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (.window (.switchTo d) new-handle)
      (when (and image-width image-height)
        (.setPosition (.window (.manage d)) (Point. image-width image-height))
        (.setSize (.window (.manage d)) (Dimension. image-width (+
                                                                  (if (= :firefox (:engine @state/options)) 75 0)
                                                                  image-height))))
      (let [startup-error (try
                            (let [url-encoded-data (add-data-text-html-base64-prefix (util/str-to-b64 (html-templates/create-svg-html svg)))]
                              (.get d url-encoded-data))
                            (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            [screenshot screenshot-error] (when-not startup-error
                                            (try
                                              [(.getScreenshotAs (cast TakesScreenshot d) OutputType/BYTES) nil]
                                              (catch Exception e
                                                [nil (str "Failed to make screenshot\n" (.getMessage e))])))

            error (first (filter some? [startup-error screenshot-error]))]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)
        (if error
          {:ok false :result error}
          {:ok true :result screenshot})))

    (catch Exception e
      (timbre/error "Exec svg to png error: " e)
      {:ok false :result (str "Exec svg to png error: " e)})))


(defn svg-to-png-cmd [svg options]
  (if-let [driver (common/create-driver)]
    (let [svg (rasterizator/clear-svg svg)
          result (exec-svg-to-png driver svg options)]

      (when (false? (:ok result))
        (common/exit driver 1 (:result result)))

      (.quit driver)
      result)
    {:ok false :result "Driver isn't available\n"}))


(defn svg-to-png-server [svg options]
  (if-let [{:keys [driver use-count]} (common/get-free-driver)]
    (let [svg (rasterizator/clear-svg svg)
          result (exec-svg-to-png driver svg options)]
      (if (:ok result)
        (common/return-driver driver (inc use-count))
        (do
          (try (.quit driver)
               (catch Exception e (timbre/error "Quit driver error: " e)))
          (common/return-new-driver)))

      result)
    {:ok false :result "Driver isn't available\n"}))


(defn svg-to-png [svg exit options]
  (if exit
    (svg-to-png-cmd svg options)
    (svg-to-png-server svg options)))