(ns export-server.browser.etaoin.html-to-png
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [export-server.data.state :as state]
            [taoensso.timbre :as timbre]
            [export-server.browser.etaoin.common :as common]))


;=======================================================================================================================
; HTML --> PNG
;=======================================================================================================================
(defn exec-html-to-png [d file exit-on-error width height svg-type?]
  (let [prev-handles (get-window-handles d)
        prev-handle (first prev-handles)]
    (js-execute d "window.open(\"\")")
    (let [new-handles (get-window-handles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))]
      (switch-window d new-handle)
      (when (and width height)
        (set-window-size d width (+
                                   (if (= :firefox (:engine @state/options)) 75 0)
                                   height)))

      (timbre/info "Open file:" file)
      (let [startup (go d file)

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (not-empty (js-execute d "return document.getElementsByTagName(\"svg\");"))
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
              (js-execute d "return document.getElementsByTagName(\"svg\")[0].outerHTML;")
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (screenshot d nil)

            error (some #(when (not (nil? %)) %) [startup])]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)

        (if error
          (if exit-on-error
            (common/exit d 1 error)
            {:ok false :result error})
          {:ok true :result (if svg-type? svg screenshot)})))))


(defn html-to-png [file quit-ph exit-on-error width height & [svg-type?]]
  (if-let [driver (if quit-ph (common/create-driverr) (common/get-free-driver))]
    (let [png-result (exec-html-to-png driver file exit-on-error width height svg-type?)]
      (if quit-ph (quit driver) (common/return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))
