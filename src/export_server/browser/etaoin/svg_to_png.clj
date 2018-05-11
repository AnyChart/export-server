(ns export-server.browser.etaoin.svg-to-png
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [export-server.browser.etaoin.common :as common]
            [export-server.data.state :as state]
            [export-server.utils.util :as util]
            [export-server.browser.templates :as templates]
            [taoensso.timbre :as timbre]
            [export-server.utils.rasterizator :as rasterizator]))



;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn add-data-text-html-base64-prefix [s64]
  (str "data:text/html;base64," s64))


(defn add-data-text-html-prefix [s64]
  (str "data:text/html," s64))


(defn- exec-svg-to-png [d svg exit-on-error options]
  (let [prev-handles (get-window-handles d)
        prev-handle (first prev-handles)]
    (js-execute d "window.open(\"\")")
    (let [new-handles (get-window-handles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (switch-window d new-handle)
      (when (and (:image-width options) (:image-height options))
        (set-window-size d (:image-width options) (+
                                                    (if (= :firefox (:engine @state/options)) 75 0)
                                                    (:image-height options))))
      (let [startup (try
                      (let [url-encoded-data (add-data-text-html-base64-prefix (util/str-to-b64 (templates/create-svg-html svg)))]
                        (go d url-encoded-data))
                      (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            screenshot (screenshot d nil)

            shutdown
            (try
              (js-execute d "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}")
              (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))

            error (some #(when (not (nil? %)) %) [startup shutdown])]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          {:ok true :result screenshot})))))


(defn svg-to-png [svg quit-ph exit-on-error options]
  (if-let [driver (if quit-ph (common/create-driverr) (common/get-free-driver))]
    (let [svg (rasterizator/clear-svg svg)
          png-result (exec-svg-to-png driver svg exit-on-error options)]
      (if quit-ph (quit driver) (common/return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))
