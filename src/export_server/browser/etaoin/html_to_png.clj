(ns export-server.browser.etaoin.html-to-png
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [export-server.data.state :as state]
            [taoensso.timbre :as timbre]
            [export-server.browser.etaoin.common :as common]
            [export-server.browser.image-resizer :as image-resizer]))


;=======================================================================================================================
; HTML --> PNG
;=======================================================================================================================
(defn exec-html-to-png [d file options svg-type?]
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
            ;; we need to resize (on white background) cause FIREFOX crop height and it has white background
            screenshot (if (= :firefox (:engine @state/options))
                         (image-resizer/resize-image screenshot options)
                         screenshot)

            error (first (filter some? [startup]))]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)

        (if error
          {:ok false :result error}
          {:ok     true
           :result (if svg-type? svg screenshot)
           :png    screenshot
           :svg    svg})))
    (catch Exception e
      (timbre/error "Exec html to png error: " e)
      {:ok false :result (str "Exec html to png error: " e)})))



(defn html-to-png-cmd [file options & [svg-type?]]
  (if-let [driver (common/create-driverr)]

    (let [result (exec-html-to-png driver file options svg-type?)]

      (when (false? (:ok result))
        (common/exit driver 1 (:result result)))

      (quit driver)

      result)
    {:ok false :result "Driver isn't available\n"}))


(defn html-to-png-server [file options & [svg-type?]]
  (if-let [{:keys [driver use-count]} (common/get-free-driver)]

    (let [result (exec-html-to-png driver file options svg-type?)]
      (if (:ok result)
        ;(let [new-use-count (inc use-count)]
        ;  (if (> new-use-count common/max-use-count)
        ;    (common/return-new-driver)
        ;    (common/return-driver driver new-use-count)))
        (common/return-driver driver (inc use-count))
        (do
          (try (quit driver)
               (catch Exception e (timbre/error "Quit driver error: " e)))
          (common/return-new-driver)))
      result)
    {:ok false :result "Driver isn't available\n"}))


(defn html-to-png [file exit options & [svg-type?]]
  (if exit
    (html-to-png-cmd file exit options svg-type?)
    (html-to-png-server file exit options svg-type?)))