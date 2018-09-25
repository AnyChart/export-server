(ns export-server.core
  (:import (java.util Properties)
           (java.nio.charset Charset))
  (:use [compojure.route :only [not-found]]
        org.httpkit.server
        compojure.core)
  (:require [ring.util.response]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [export-server.data.state :as state]
            [export-server.handlers.web-handlers :as web]
            [export-server.handlers.cmd-handlers :as cmd]
            [export-server.browser.core :as browser]
            [export-server.sharing.core :as sharing]
            [export-server.sharing.twitter :as twitter]
            [export-server.sharing.storage :as storage :refer [create-storage init]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))


;====================================================================================
; Main utils
;====================================================================================
(defn get-project-version
  ([] (get-project-version "export-server" "export-server"))
  ([groupid artifact] (-> (doto (Properties.)
                            (.load (-> "META-INF/maven/%s/%s/pom.properties"
                                       (format groupid artifact)
                                       (io/resource)
                                       (io/reader))))
                          (.get "version"))))


(def server-name (str "AnyChart Export Server " (get-project-version)))


(defn init-logger [log-file-name]
  (clojure.java.io/delete-file log-file-name :quiet)
  (timbre/merge-config!
    {:appenders {:spit (appenders/spit-appender {:fname log-file-name})}})
  ; Set the lowest-level to output as :debug
  (timbre/set-level! :debug)
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (timbre/error ex "Uncaught exception on" (.getName thread))))))


;====================================================================================
; Common usage summary
;====================================================================================
(defn usage [common-summary server-summary cmd-summary]
  (->> [server-name
        ""
        "Usage: java -jar anychart-export.jar action [options]"
        "Actions:"
        "  server    Start a new instance of AnyChart Export Server."
        "            Use --help arg with action for more info."
        ""
        "  cmd       Run Export Server once"
        "            Use --help arg with action for more info."
        ""
        "Common options:"
        common-summary
        ""
        "Server options:"
        server-summary
        ""
        "CMD options:"
        cmd-summary
        ""
        "Please, see http://docs.anychart.com for more info."
        ]
       (string/join \newline)))


;====================================================================================
; Server usage summary
;====================================================================================
(defn server-usage [options-summary]
  (->> [server-name
        ""
        "Action: server"
        "Usage: java -jar anychart-export.jar server [options]"
        "See https://github.com/AnyChart/export-server for HTTP API."
        ""
        "Options:"
        options-summary
        ""
        "Please, see http://docs.anychart.com for more info."
        ]
       (string/join \newline)))


;====================================================================================
; Command Line usage summary
;====================================================================================
(defn cmd-usage [options-summary]
  (->> [server-name
        ""
        "Action: cmd"
        "Usage: java -jar anychart-export.jar cmd [options]"
        ""
        "Options:"
        options-summary
        ""
        "Please, see http://docs.anychart.com for more info."
        ]
       (string/join \newline)))


(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))


;====================================================================================
; Options
;====================================================================================
(def common-options
  [["-C" "--config PATH" "Path to config"
    :default nil]
   ["-e" "--engine BROWSER" "Headless browser: phantom, chrome or firefox"
    :parse-fn keyword
    :validate [(fn [engine] (some #(= engine %) [:phantom :chrome :firefox]))]]
   ["-v" "--version" "Print version, can be used without action"]
   ["-h" "--help" "Print help"]])

(def common-summary (:summary (parse-opts nil common-options)))


(def server-options
  [["-P" "--port PORT" "Port number for the server."
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-H" "--host HOST" "Ip, if has many ips to bind."]

   ["-F" "--log FILE" "File for server logging."]

   ["-a" "--allow-scripts-executing ALLOW_SCRIPTS_EXECUTING" "Allow to execute violent scripts in phantom js."
    :parse-fn #(or (= "true" %) (= "1" %) (= "y" %) (= "yes" %))]

   ;Saving image or pdf to folder
   ["-z" "--saving-folder PATH" "Path to save images or pdf"]
   ["-Z" "--saving-url-prefix PREFIX" "URL prefix will be returned to request"]

   ;; sharing
   [nil "--sharing-port PORT" "Sharing mysql database port" :parse-fn #(Integer/parseInt %)]
   [nil "--sharing-db NAME" "Sharing mysql database name"]
   [nil "--sharing-user USER" "Sharing mysql database user"]
   [nil "--sharing-password PASSWORD" "Sharing mysql database password"]

   ;; twitter
   [nil "--twitter-key KEY" "Twitter application key"]
   [nil "--twitter-secret SECRET" "Twitter application secret"]
   [nil "--twitter-callback" "Twitter application callback URL"]])


(def server-summary (:summary (parse-opts nil server-options)))
(def full-server-summary (:summary (parse-opts nil (concat common-options server-options))))


(def cmd-options
  [["-s" "--script SCRIPT" "JavaScript String to Execute."]

   ["-i" "--input-file INPUT_FILE" "JavaScript file to Execute"]

   [nil "--svg SVG" "SVG string to Execute"]

   [nil "--svg-file SVG_FILE" "SVG file to Execute"]

   [nil "--html-file HTML_FILE" "HTML page file to Execute"]

   ["-o" "--output-file OUTPUT_FILE" "Output File name, file extentions is optional."]

   ["-p" "--output-path OUTPUT_PATH" "Output File Directory"]

   ["-t" "--type TYPE" "Type of the output file."
    :validate [#(contains? #{"png" "jpg" "svg" "pdf"} %) "Type must be one of the following types: png, jpg, svg, pdf"]]

   ["-c" "--container-id CONTAINER_ID" "Container id."]

   ["-W" "--container-width CONTAINER_WIDTH" "Container width."]

   ["-L" "--container-height CONTAINER_HEIGHT" "Container height"]

   ;Export Images Args--------------------------------------------------------------------------------------------------
   ["-w" "--image-width IMAGE_WIDTH" "Image width."
    :parse-fn #(Integer/parseInt %)]

   ["-l" "--image-height IMAGE_HEIGHT" "Image height"
    :parse-fn #(Integer/parseInt %)]

   ["-f" "--force-transparent-white FORCE_TRANSPARENT_WHITE" "Force transparent to white"]

   ["-q" "--jpg-quality JPG_QUALITY" "Image quality,"
    :parse-fn #(Float/parseFloat %)]

   ;Export PDF Args--------------------------------------------------------------------------------------------------
   ["-S" "--pdf-size PDF-SIZE" "PDF Size"
    :parse-fn #(keyword %)]

   ["-X" "--pdf-width PDF-WIDTH" "Pdf width"
    :parse-fn #(Integer/parseInt %)]

   ["-Y" "--pdf-height PDF-HEIGHT" "Pdf height"
    :parse-fn #(Integer/parseInt %)]

   ["-x" "--pdf-x PDF-X" "Pdf X"
    :parse-fn #(Integer/parseInt %)]

   ["-y" "--pdf-y PDF-Y" "Pdf Y"
    :parse-fn #(Integer/parseInt %)]

   ["-O" "--pdf-landscape PDF-LANDSCAPE" "PDF Orientation"]])

(def cmd-summary (:summary (parse-opts nil cmd-options)))
(def full-cmd-summary (:summary (parse-opts nil (concat common-options cmd-options))))

(def all-options
  (concat common-options server-options cmd-options))


;====================================================================================
; Server Actions
;====================================================================================
(defroutes app-routes
           (route/resources "/")
           (GET "/status" [] "ok")
           (POST "/status" [] "ok")
           (POST "/sharing/twitter" [] web/sharing-twitter)
           (GET "/sharing/twitter_oauth" [] twitter/twitter-oauth)
           (POST "/sharing/twitter_confirm" [] twitter/twitter-confirm)
           (POST "/png" [] web/png)
           (POST "/jpg" [] web/jpg)
           (POST "/svg" [] web/svg)
           (POST "/pdf" [] web/pdf)
           (POST "/xml" [] web/xml)
           (POST "/json" [] web/json)
           (POST "/csv" [] web/csv)
           (POST "/xlsx" [] web/xlsx)
           (route/not-found "<p>Page not found.</p>"))


;(def app (-> app-routes wrap-params))
(def app (-> app-routes wrap-params (wrap-session {:store (create-storage)})))


(defn shutdown-server []
  (timbre/info "Shutdown...")
  (state/stop-server!)
  (browser/stop-drivers))


(defn start-server [options summary]
  (if (:help options) (exit 0 (server-usage summary)))
  (when (:log options)
    (init-logger (:log options)))
  (timbre/info (str "Starting export server on " (:host options) ":" (:port options)))
  (if (sharing/init options)
    (timbre/info "Sharing initialiazed")
    (timbre/warn "Sharing did not initialize. Provide both twitter-* and sharing-* options."))
  (browser/setup-drivers)
  (state/set-server! (run-server app {:port (:port options) :ip (:host options)}))
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-server)))


;====================================================================================
; Cmd Actions
;====================================================================================
(defn cmd-export [options summary]
  (if (:help options)
    (exit 0 (cmd-usage summary)))
  (let [script (:script options)
        file (:input-file options)
        svg (:svg options)
        svg-file (:svg-file options)
        html-file (:html-file options)]
    (cond
      (and (nil? script)
           (nil? file)
           (nil? svg)
           (nil? svg-file)
           (nil? html-file))
      (exit 1 (error-msg ["script, file or HTML page should be specified in 'cmd' mode."]))

      (and file
           (not (.exists (io/file file))))
      (exit 1 (error-msg ["Input File not exists."]))

      (and svg-file
           (not (.exists (io/file svg-file))))
      (exit 1 (error-msg ["Svg file not exists."]))

      (and html-file
           (not (.exists (io/file html-file))))
      (exit 1 (error-msg ["HTML page file not exists."]))

      (or script file)
      (cmd/script->export options)

      (or svg svg-file)
      (cmd/svg->export options)

      html-file
      (cmd/html->export options))))


(defn set-default-charset []
  (System/setProperty "file.encoding" "UTF-8")
  (doto (.getDeclaredField Charset "defaultCharset") (.setAccessible true) (.set nil nil))
  ;(timbre/info "Charset: " (System/getProperty "file.encoding"))
  )


;====================================================================================
; Main
;====================================================================================
(defn -main [& args]
  (set-default-charset)
  (let [{:keys [options arguments errors summary]} (parse-opts args all-options)]
    (cond
      (:version options) (exit 0 server-name)
      errors (exit 1 (error-msg errors))
      (nil? (state/init (first arguments) options)) (exit 1 "Can't read config file"))
    (let [options @state/options
          mode (:mode options)]
      (reset! web/allow-script-executing (:allow-scripts-executing options))
      (case mode
        "server" (start-server options full-server-summary)
        "cmd" (cmd-export options full-cmd-summary)
        (exit (if (:help options) 0 1)
              (usage common-summary server-summary cmd-summary))))))
