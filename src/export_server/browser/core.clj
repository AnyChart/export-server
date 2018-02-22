(ns export-server.browser.core
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [clojure.java.io :as io]
            [tupelo.base64 :as b64]
            [export-server.utils.rasterizator :as rasterizator]))

;=======================================================================================================================
; Screenshot etaoin override to return image, not write it to a file
;=======================================================================================================================
(defmethod screenshot :chrome
  [driver file]
  (with-resp driver :get
             [:session (:session @driver) :screenshot]
             nil
             resp
             (if-let [b64str (-> resp :value not-empty)]
               (b64/decode-str->bytes b64str)
               (slingshot.slingshot/throw+ {:type    :etaoin/screenshot
                                            :message "Empty screenshot"
                                            :driver  @driver}))))

(defmethod screenshot :firefox
  [driver file]
  (with-resp driver :get
             [:session (:session @driver) :screenshot]
             nil
             resp
             (if-let [b64str (-> resp :value not-empty)]
               (b64/decode-str->bytes b64str)
               (slingshot.slingshot/throw+ {:type    :etaoin/screenshot
                                            :message "Empty screenshot"
                                            :driver  @driver}))))

(defmethod screenshot :phantom
  [driver file]
  (with-resp driver :get
             [:session (:session @driver) :screenshot]
             nil
             resp
             (if-let [b64str (-> resp :value not-empty)]
               (b64/decode-str->bytes b64str)
               (slingshot.slingshot/throw+ {:type    :etaoin/screenshot
                                            :message "Empty screenshot"
                                            :driver  @driver}))))


;=======================================================================================================================
; Initialization
;=======================================================================================================================
(defonce drivers (atom []))
(defonce drivers-queue nil)

(defn- get-free-driver []
  (.poll drivers-queue))

(defn- return-driver [driver]
  (.add drivers-queue driver))

(defn build-driver [] (firefox-headless))

(defn setup-drivers []
  (reset! drivers [(build-driver) (build-driver) (build-driver) (build-driver)])
  (alter-var-root (var drivers-queue)
                  (fn [_]
                    (let [queue (java.util.concurrent.ConcurrentLinkedQueue.)]
                      (doseq [driver @drivers]
                        (.add queue driver))
                      queue))))

(defn stop-drivers []
  (doseq [driver @drivers]
    (try
      (quit driver)
      (catch Exception e nil))))

(defn exit [driver status msg]
  (quit driver)
  (println msg)
  (System/exit status))


;=======================================================================================================================
; Script --> SVG | PNG
;=======================================================================================================================
(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))
(def replacesvgsize (slurp (io/resource "js/replacesvgsize.min.js")))


(defn- exec-script-to-png [d script exit-on-error options type]
  (let [prev-handles (get-window-handles d)]
    (js-execute d "window.open(\"\")")
    (let [new-handles (get-window-handles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]

      (switch-window d new-handle)
      ;(prn "prev handles: " prev-handles)
      ;(prn "Current: " (.getWindowHandle (:webdriver d)))
      (let [startup
            (try
              (js-execute d "document.getElementsByTagName(\"body\")[0].style.margin = 0;
                                 document.body.innerHTML = '<div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>'"
                          (:container-id options) (:container-width options) (:container-height options))
              (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))
            binary
            (try
              (prn "Exec binary!")
              (js-execute d anychart-binary)
              (catch Exception e (str "Failed to execute AnyChat Binary File\n" (.getMessage e))))
            script
            (try
              (js-execute d script)
              (catch Exception e (str "Failed to execute Script\n" (.getMessage e))))

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (not-empty (js-execute d "return document.getElementsByTagName(\"svg\");"))
                    nil
                    (if (> (System/currentTimeMillis) (+ now 2000))
                      "error"
                      (do
                        (Thread/sleep 10)
                        (recur))))))
              (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))

            resize nil
            ;(try
            ;  (execute-script d replacesvgsize)
            ;  (catch Exception e (str "Failed to execute ReplaceSvgSize\n" (.getMessage e))))

            svg
            (try
              (js-execute d "return document.getElementsByTagName(\"svg\")[0].outerHTML;")
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot-bytes (screenshot d nil)

            shoutdown
            (try
              (js-execute d "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}", [])
              (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))
            error (some #(when (not (nil? %)) %) [startup binary script shoutdown waiting resize])]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)

        ;(prn "End handles: " (.getWindowHandles (:webdriver d)))
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))

        (if error
          (if exit-on-error
            (exit d 1 error)
            {:ok false :result error})
          (case type
            :png {:ok true :result screenshot-bytes}
            :svg {:ok true :result svg}))))))


(defn script-to-png [script quit-ph exit-on-error options type]
  (if-let [driver (if quit-ph (build-driver) (get-free-driver))]
    (let [svg (exec-script-to-png driver script exit-on-error options type)]
      (if quit-ph (quit driver) (return-driver driver))
      svg)
    {:ok false :result "Driver isn't available\n"}))


;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn- exec-svg-to-png [d svg exit-on-error width height]
  (let [prev-handles (get-window-handles d)]
    (js-execute d "window.open(\"\")")
    (let [new-handles (get-window-handles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]

      (prn "SWITCH: " d new-handle)
      (switch-window d new-handle)
      (when (and width height)
        (prn "SIZE: " width height)
        (set-window-size d width height))
      (let [startup
            (try
              (js-execute d "document.body.style.margin = 0;
                                 document.body.innerHTML = arguments[0]"
                          svg width height)
              (catch Exception e (str "Failed to execute Startup Script\n" (.getMessage e))))

            screenshot (screenshot d nil)

            shoutdown
            (try
              (js-execute d "while (document.body.hasChildNodes()){document.body.removeChild(document.body.lastChild);}")
              (catch Exception e (str "Failed to execute Shoutdown Script\n" (.getMessage e))))
            error (some #(when (not (nil? %)) %) [startup shoutdown])]

        (js-execute d "window.close(\"\")")
        (switch-window d prev-handle)
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))
        (if error
          (if exit-on-error
            (exit d 1 error)
            {:ok false :result error})
          {:ok true :result screenshot})))))


(defn svg-to-png [svg quit-ph exit-on-error width height]
  (if-let [driver (if quit-ph (build-driver) (get-free-driver))]
    (let [svg (rasterizator/clear-svg svg)
          png-result (exec-svg-to-png driver svg exit-on-error width height)]
      (if quit-ph (quit driver) (return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))