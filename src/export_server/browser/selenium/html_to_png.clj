(ns export-server.browser.selenium.html-to-png
  (:require [taoensso.timbre :as timbre]
            [export-server.browser.selenium.common :as common]
            [export-server.data.state :as state]
            [export-server.browser.image-resizer :as image-resizer])
  (:import (org.openqa.selenium Dimension OutputType By)))


;=======================================================================================================================
; HTML --> PNG
;=======================================================================================================================
(defn exec-html-to-png [d file exit-on-error options svg-type?]
  (let [prev-handles (.getWindowHandles d)
        prev-handle (first prev-handles)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (.window (.switchTo d) new-handle)
      (when (and (:image-width options) (:image-height options))
        (.setSize (.window (.manage d)) (Dimension. (:image-width options)
                                                    (+
                                                      (if (= :firefox (:engine @state/options)) 75 0)
                                                      (:image-height options)))))

      (timbre/info "Open file:" file)
      (let [startup (.get d file)

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (seq (.findElements d (By/tagName "svg")))
                    nil
                    (if (> (System/currentTimeMillis) (+ now 5000))
                      (do
                        (Thread/sleep 10)
                        nil)
                      (do
                        (Thread/sleep 10)
                        (recur))))))
              (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))

            svg
            (try
              (.executeScript d "return document.getElementsByTagName(\"svg\")[0].outerHTML;" (into-array []))
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (.getScreenshotAs d OutputType/BYTES)
            ;; we need to resize (on white background) cause FIREFOX crop height and it has white background
            _ (prn (:image-width options) (:image-height options))
            screenshot (if (= :firefox (:engine @state/options))
                         (image-resizer/resize-image screenshot options)
                         screenshot)

            error (some #(when (not (nil? %)) %) [startup])]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          {:ok true :result (if svg-type? svg screenshot)})))))


(defn html-to-png [file quit-ph exit-on-error options & [svg-type?]]
  (if-let [driver (if quit-ph (common/create-driver) (common/get-free-driver))]
    (let [png-result (exec-html-to-png driver file exit-on-error options svg-type?)]
      (if quit-ph (.quit driver) (common/return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))