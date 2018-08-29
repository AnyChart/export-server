(ns export-server.browser.etaoin.common
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [tupelo.base64 :as b64]
            [export-server.data.state :as state]
            [taoensso.timbre :as timbre])
  (:import (java.util.concurrent LinkedBlockingQueue)))


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
; Drivers initialization
;=======================================================================================================================
(defn create-driver-phantom [] (phantom))

(defn create-driver-chrome []
  ;(chrome-headless)
  (chrome {:args         ["--disable-gpu" "--no-sandbox"]
           :capabilities {:chromeOptions {:args ["--headless" "--disable-gpu" "--no-sandbox"]}}}))

(defn create-driver-firefox [] (firefox-headless))

(defn create-driverr []
  (case (:engine @state/options)
    :phantom (create-driver-phantom)
    :chrome (create-driver-chrome)
    :firefox (create-driver-firefox)))


;=======================================================================================================================
; Initialization
;=======================================================================================================================
(defonce drivers-num 4)
(defonce drivers-queue nil)
(defonce max-use-count 2)


(defn get-free-driver []
  (println "get free driver")
  (.take drivers-queue))


(defn put-driver [driver use-count]
  (.put drivers-queue {:driver    driver
                       :use-count use-count}))


(defn return-new-driver []
  (put-driver (create-driverr) 0))


(defn return-driver [driver use-count]
  (println "put driver: " use-count)
  (if (< use-count max-use-count)
    (put-driver driver use-count)
    (do
      (timbre/info "Recreate driver")
      (quit driver)
      (return-new-driver))))


(defn setup-drivers []
  (alter-var-root (var drivers-queue)
                  (constantly (LinkedBlockingQueue. drivers-num)))
  (dotimes [_ drivers-num]
    (return-new-driver)))


(defn stop-drivers []
  (try
    (dotimes [_ drivers-num]
     (let [{driver :driver} (get-free-driver)]
       (quit driver)))
    (catch Exception e
      (timbre/error "Stop drivers error: " e))))


(defn exit [driver status msg]
  (quit driver)
  (println msg)
  (System/exit status))