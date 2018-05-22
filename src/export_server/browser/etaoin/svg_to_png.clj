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


(defn exec-svg-to-png [d svg options]
  (try
    (let [prev-handles (get-window-handles d)
          prev-handle (first prev-handles)
          _ (js-execute d "window.open(\"\")")
          new-handles (get-window-handles d)
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

            error (first (filter some? [startup shutdown]))]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))

        (if error
          {:ok false :result error}
          {:ok true :result screenshot})))
    (catch Exception e
      (timbre/error "Exec svg to png error: " e)
      {:ok false :result (str "Exec svg to png error: " e)})))


(defn svg-to-png [svg quit-ph exit-on-error options]
  (if-let [driver (if quit-ph (common/create-driverr) (common/get-free-driver))]
    (let [svg (rasterizator/clear-svg svg)
          result (exec-svg-to-png driver svg options)]
      (when (and (false? (:ok result)) exit-on-error)
        (common/exit driver 1 (:result result)))
      (if quit-ph (quit driver) (common/return-driver driver))
      result)
    {:ok false :result "Driver isn't available\n"}))
