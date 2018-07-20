(ns export-server.browser.etaoin.common
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [tupelo.base64 :as b64]
            [export-server.data.state :as state]))


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
(defonce drivers (atom []))
(defonce drivers-queue nil)


(defn get-free-driver []
  (.poll drivers-queue))


(defn return-driver [driver]
  (.add drivers-queue driver))


(defn setup-drivers []
  (reset! drivers [(create-driverr) (create-driverr) (create-driverr) (create-driverr)])
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