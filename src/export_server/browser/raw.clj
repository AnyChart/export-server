(ns export-server.browser.raw
  (:require [clojure.java.io :as io]
            [export-server.utils.rasterizator :as rasterizator]
            [taoensso.timbre :as timbre]
            [export-server.data.state :as state])
  (:import (org.openqa.selenium.firefox FirefoxBinary FirefoxOptions FirefoxDriver FirefoxProfile)
           (org.openqa.selenium OutputType Dimension Point By TakesScreenshot)
           (org.openqa.selenium.chrome ChromeOptions ChromeDriver)
           (java.util ArrayList)
           (org.openqa.selenium.remote DesiredCapabilities)
           (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService)
           (javax.imageio ImageIO)
           (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.awt.image BufferedImage)
           (java.awt Color)))


;=======================================================================================================================
; Drivers initialization
;=======================================================================================================================
(defn- create-driver-phantom []
  (let [caps (DesiredCapabilities.)
        cliArgsCap (ArrayList.)]
    (.add cliArgsCap "--web-security=false")
    (.add cliArgsCap "--ssl-protocol=any")
    (.add cliArgsCap "--ignore-ssl-errors=true")
    (.setCapability caps PhantomJSDriverService/PHANTOMJS_CLI_ARGS cliArgsCap)
    (PhantomJSDriver. caps)))


(defn- create-driver-chrome []
  (let [opts (ChromeOptions.)
        args (ArrayList.)
        caps (DesiredCapabilities.)]
    ;(.add args "--headless")
    ;(.add args "--allow-running-insecure-content")
    ;(.add args "--ignore-certificate-errors")
    ;(.add args "--no-sandbox")
    ;(.add args "--screenshot")
    ;(.addArguments opts args)
    (.setAcceptInsecureCerts opts true)
    (.setHeadless opts true)
    ;(.setCapability caps "acceptInsecureCerts" true)
    ;(.setCapability caps "acceptSslCerts" true)
    ;(.setAcceptInsecureCerts caps  true)
    ;(.acceptInsecureCerts caps)
    (ChromeDriver. opts)))


(defn create-driver-firefox []
  (let [opts (FirefoxOptions.)
        binary (FirefoxBinary.)]
    (.addCommandLineOptions binary (into-array ["--headless"]))
    (.setBinary opts binary)
    (FirefoxDriver. opts)))

;(defn create-driver-firefox []
;  (let [opts (FirefoxOptions.)
;        binary (FirefoxBinary.)
;        profile (FirefoxProfile.)]
;    (.setAssumeUntrustedCertificateIssuer profile false)
;    (.setCapability opts "security.sandbox.content.level" "0")
;    (.setCapability opts "accept_untrusted_certs" true)
;    (.setAcceptInsecureCerts opts true)
;    (.addCommandLineOptions binary (into-array ["--headless"]))
;    (.setBinary opts binary)
;    (.setProfile opts profile)
;    (FirefoxDriver. opts)))


(defn create-driver []
  (case (:engine @state/options)
    :phantom (create-driver-phantom)
    :chrome (create-driver-chrome)
    :firefox (create-driver-firefox)))

;=======================================================================================================================
; Drivers pool management
;=======================================================================================================================


(defonce drivers (atom []))
(defonce drivers-queue nil)

(defn- get-free-driver []
  (.poll drivers-queue))

(defn- return-driver [driver]
  (.add drivers-queue driver))

(defn setup-drivers []
  (timbre/info "Headless browser:" (name (:engine @state/options)))
  (reset! drivers [(create-driver) (create-driver) (create-driver) (create-driver)])
  (alter-var-root (var drivers-queue)
                  (fn [_]
                    (let [queue (java.util.concurrent.ConcurrentLinkedQueue.)]
                      (doseq [driver @drivers]
                        (.add queue driver))
                      queue))))

(defn stop-drivers []
  (doseq [driver @drivers]
    (try
      (.quit driver)
      (catch Exception e nil))))

(defn exit [driver status msg]
  (.quit driver)
  (println msg)
  (System/exit status))


(defn resize-image [screenshot options]
  (let [img (ImageIO/read (ByteArrayInputStream. screenshot))

        ; new-img (.getSubimage img 0 0 (:image-width options) (:image-height options))
        new-img (BufferedImage. (:image-width options) (:image-height options) (.getType img))
        g (.getGraphics new-img)
        _ (.setColor g Color/WHITE)
        _ (.fillRect g 0 0 (:image-width options) (:image-height options))
        _ (.drawImage g img 0 0
                      (min (.getWidth img) (:image-width options))
                      (min (.getHeight img) (:image-height options))
                      nil)
        _ (.dispose g)

        baos (ByteArrayOutputStream.)
        _ (ImageIO/write new-img "png" baos)
        bytes (.toByteArray baos)]
    (.close baos)
    bytes))


;=======================================================================================================================
; Script --> SVG | PNG
;=======================================================================================================================
(def anychart-binary (slurp (io/resource "js/anychart-bundle.min.js")))
(def replacesvgsize (slurp (io/resource "js/replacesvgsize.min.js")))


(defn get-svg [d]
  ;(prn "SvG:" (.executeScript d "return document.getElementsByTagName(\"svg\")[0].outerHTML;" (into-array [])))
  ;(prn "SvG:" (.getAttribute (first (.findElements d (By/tagName "svg"))) "innerHTML"))
  ;(prn "SvG:" (.getAttribute (second (.findElements d (By/tagName "svg"))) "innerHTML"))
  ;(prn "SvG id:" (.getAttribute (.findElement d (By/id (:container-id options))) "innerHTML"))
  ;(prn "SvG id:" (.getAttribute (.findElement d (By/id (:container-id options))) "outerHTML"))
  ;(prn "SvG xpath:" (.findElement d (By/xpath "//*[local-name() = 'svg']")))
  (or (try
        (let [inner (.getAttribute (.findElement d (By/cssSelector "#container div")) "innerHTML")
              svg-end (.lastIndexOf inner "</svg>")
              svg (subs inner 0 (+ 6 svg-end))]
          svg)
        (catch Exception e nil))
      (.executeScript d "return document.getElementsByTagName(\"svg\")[0].outerHTML;" (into-array [])))
  ;(prn "SvG xpath:" (.getAttribute (.findElement d (By/cssSelector "#container svg")) "innerHTML") )
  ;(prn "SvG3:")
  ;(println "SvG3:" (.getAttribute (first (.findElements d (By/tagName "body"))) "innerHTML"))
  ;(prn "OUter:" (.executeScript d "return document.getElementsByTagName(\"svg\")[0].innerHTML;" (into-array [])))
  ;(prn "OUter:" (.executeScript d "return 1 + 3;" (into-array [])))
  )


(defn- exec-script-to-png [d script exit-on-error options type]
  ;(prn (:image-width options) (:image-height options))
  ;(prn (:container-width options) (:container-height options))
  (let [prev-handles (.getWindowHandles d)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]
      (.window (.switchTo d) new-handle)

      (.setSize (.window (.manage d)) (Dimension. (:image-width options) (+ (:image-height options)
                                                                            (if (= :firefox (:engine @state/options)) 75 0))))

      ;(prn "prev handles: " prev-handles)
      ;(prn "Current: " (.getWindowHandle (:webdriver d)))
      (let [startup
            (try
              (.executeScript d "document.getElementsByTagName(\"body\")[0].style.margin = 0;
                                 document.body.innerHTML = '<style>.anychart-credits{display:none;}</style><div id=\"' + arguments[0] + '\" style=\"width:' + arguments[1] + ';height:' + arguments[2] + ';\"></div>'"
                              (into-array [(:container-id options) (:container-width options) (:container-height options)]))
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
                    nil
                    (if (> (System/currentTimeMillis) (+ now 2000))
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
              (get-svg d)
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (.getScreenshotAs d OutputType/BYTES)
            ;; we need to resize (on white background) cause FIREFOX crop height and it has white background
            screenshot (resize-image screenshot options)

            error (some #(when (not (nil? %)) %) [startup binary script waiting])]

        (.executeScript d "window.close(\"\")" (into-array []))

        (.window (.switchTo d) prev-handle)
        ;(prn "End handles: " (.getWindowHandles (:webdriver d)))
        ;(with-open [out (output-stream (clojure.java.io/file "/media/ssd/sibental/export-server-data/script-to-png.png"))]
        ;  (.write out screenshot))
        ;(prn "SVG: " svg)

        (if error
          (if exit-on-error
            (exit d 1 error)
            {:ok false :result error})
          (case type
            :png {:ok true :result screenshot}
            :svg {:ok true :result svg}))))))


(defn script-to-png [script quit-ph exit-on-error options type]
  (if-let [driver (if quit-ph (create-driver) (get-free-driver))]
    (let [svg (exec-script-to-png driver script exit-on-error options type)]
      (if quit-ph (.quit driver) (return-driver driver))
      svg)
    {:ok false :result "Driver isn't available\n"}))


;=======================================================================================================================
; SVG --> PNG
;=======================================================================================================================
(defn- exec-svg-to-png [d svg exit-on-error width height]
  (let [prev-handles (.getWindowHandles d)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]
      (.window (.switchTo d) new-handle)
      (when (and width height)
        (.setPosition (.window (.manage d)) (Point. width height))
        (.setSize (.window (.manage d)) (Dimension. width (+
                                                            (if (= :firefox (:engine @state/options)) 75 0)
                                                            height))))
      (let [startup
            (try
              (.executeScript d "document.body.style.margin = 0;
                                 document.body.innerHTML = arguments[0]"
                              (into-array [svg]))
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
            (exit d 1 error)
            {:ok false :result error})
          {:ok true :result screenshot})))))


(defn svg-to-png [svg quit-ph exit-on-error width height]
  (if-let [driver (if quit-ph (create-driver) (get-free-driver))]
    (let [svg (rasterizator/clear-svg svg)
          png-result (exec-svg-to-png driver svg exit-on-error width height)]
      (if quit-ph (.quit driver) (return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))


;=======================================================================================================================
; HTML --> PNG
;=======================================================================================================================
(defn exec-html-to-png [d file exit-on-error width height svg-type?]
  (let [prev-handles (.getWindowHandles d)]
    (.executeScript d "window.open(\"\")" (into-array []))
    (let [new-handles (.getWindowHandles d)
          new-handle (first (clojure.set/difference (set new-handles) (set prev-handles)))
          prev-handle (first prev-handles)]
      (.window (.switchTo d) new-handle)
      (when (and width height)
        (.setSize (.window (.manage d)) (Dimension. width height)))

      (timbre/info "Open file:" file)
      (let [startup (.get d file)

            waiting
            (try
              (let [now (System/currentTimeMillis)]
                (loop []
                  (if (seq (.findElements d (By/tagName "svg")))
                    nil
                    (if (> (System/currentTimeMillis) (+ now 2000))
                      "SVG waiting timeout"
                      (do
                        (Thread/sleep 10)
                        (recur))))))
              (catch Exception e (str "Failed to wait for SVG\n" (.getMessage e))))

            svg
            (try
              (.executeScript d "return document.getElementsByTagName(\"svg\")[0].outerHTML;" (into-array []))
              (catch Exception e (str "Failed to take SVG Structure\n" (.getMessage e))))

            screenshot (.getScreenshotAs d OutputType/BYTES)

            error (some #(when (not (nil? %)) %) [startup])]

        (.executeScript d "window.close(\"\")" (into-array []))
        (.window (.switchTo d) prev-handle)

        (if error
          (if exit-on-error
            (exit d 1 error)
            {:ok false :result error})
          {:ok true :result (if svg-type? svg screenshot)})))))


(defn html-to-png [file quit-ph exit-on-error width height & [svg-type?]]
  (if-let [driver (if quit-ph (create-driver) (get-free-driver))]
    (let [png-result (exec-html-to-png driver file exit-on-error width height svg-type?)]
      (if quit-ph (.quit driver) (return-driver driver))
      png-result)
    {:ok false :result "Driver isn't available\n"}))