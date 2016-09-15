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
            [export-server.state :as state]
            [export-server.web-handlers :as web]
            [export-server.cmd-handlers :as cmd]
            [export-server.utils.phantom :as browser]
            ;[export-server.utils.jbrowser :as browser]
            [export-server.utils.config :as config]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))

;====================================================================================
; Main utils
;====================================================================================
(defn jar-location
  "if run locally returns clojure.jar path"
  [& [ns]]
  (-> (or ns (class *ns*)) .getProtectionDomain .getCodeSource .getLocation .getPath clojure.java.io/file .getParent))

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
; Server Usage
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
; Command Line Usage
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
; Common Usage
;====================================================================================
(def common-options
  [
   ;Server Args--------------------------------------------------------------------------------------------
   ["-P" "--port PORT" "Port number for the server."
    :default (:port config/defaults)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--host HOST" "Ip, if has many ips to bind."
    :default (:host config/defaults)
    ]
   ["-F" "--log FILE" "File for server logging."
    :default (:log config/defaults)
    ]
   ["-a" "--allow-scripts-executing ALLOW_SCRIPTS_EXECUTING" "Allow to execute violent scripts in phantom js."
    :parse-fn #(or (= "true" %) (= "1" %) (= "y" %) (= "yes" %))
    :default true
    ]

   ;Command Line Common Args--------------------------------------------------------------------------------------------
   ["-s" "--script SCRIPT" "JavaScript String to Execute."
    :default nil
    ]
   ["-i" "--input-file INPUT_FILE" "JavaScript file to Execute"
    :default nil
    ]
   ["-o" "--output-file OUTPUT_FILE" "Output File name, file extentions is optional."
    :default "anychart"
    ]
   ["-p" "--output-path OUTPUT_PATH" "Output File Directory"
    :default ""
    ]
   ["-t" "--type TYPE" "Type of the output file."
    :default (:type config/defaults)
    :validate [#(contains? #{"png" "jpg" "svg" "pdf"} %) "Type must be one of the following types: png, jpg, svg, pdf"]]

   ["-c" "--container-id CONTAINER_ID" "Container id."
    :default (:container-id config/defaults)
    ]

   ["-W" "--container-width CONTAINER_WIDTH" "Container width."
    :default (:container-width config/defaults)
    ]

   ["-L" "--container-height CONTAINER_HEIGHT" "Container height"
    :default (:container-height config/defaults)
    ]


   ;Export Images Args--------------------------------------------------------------------------------------------------
   ["-w" "--image-width IMAGE_WIDTH" "Image width."
    :default (:image-width config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]

   ["-l" "--image-height IMAGE_HEIGHT" "Image height"
    :default (:image-height config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]
   ["-f" "--force-transparent-white FORCE_TRANSPARENT_WHITE" "Force transparent to white"
    :default (:force-transparent-white config/defaults)
    ]

   ["-q" "--jpg-quality JPG_QUALITY" "Image quality,"
    :default (:jpg-quality config/defaults)
    :parse-fn #(Float/parseFloat %)
    ]


   ;Export PDF Args--------------------------------------------------------------------------------------------------
   ["-S" "--pdf-size PDF-SIZE" "PDF Size"
    :default (:pdf-size config/defaults)
    :parse-fn #(keyword %)
    ]

   ["-X" "--pdf-width PDF-WIDTH" "Pdf width"
    :default (:pdf-width config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]

   ["-Y" "--pdf-height PDF-HEIGHT" "Pdf height"
    :default (:pdf-width config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]

   ["-x" "--pdf-x PDF-X" "Pdf X"
    :default (:pdf-x config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]

   ["-y" "--pdf-y PDF-Y" "Pdf Y"
    :default (:pdf-y config/defaults)
    :parse-fn #(Integer/parseInt %)
    ]

   ["-O" "--pdf-landscape PDF-LANDSCAPE" "PDF Orientation"
    :default (:pdf-landscape config/defaults)
    ]

   ;Saving image or pdf to folder
   ["-z" "--saving-folder PATH" "Path to save images or pdf"
    :default (str (jar-location) "/save")
    ]
   ["-Z" "--saving-url-prefix PREFIX" "URL prefix will be returned to request"
    :default ""
    ]

   ;Export PDF Args--------------------------------------------------------------------------------------------------
   ["-v" "--version" "Print version, can be used without action"]
   ["-h" "--help"]
   ])

(defn usage []
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
        "Please, see http://docs.anychart.com for more info."
        ]
       (string/join \newline)))


;====================================================================================
; Server Actions
;====================================================================================
(defroutes app-routes
           (GET "/status" [] "ok")
           (POST "/status" [] "ok")
           (POST "/png" [] web/png)
           (POST "/jpg" [] web/jpg)
           (POST "/svg" [] web/svg)
           (POST "/pdf" [] web/pdf)
           (POST "/xml" [] web/xml)
           (POST "/json" [] web/json)
           (POST "/csv" [] web/csv)
           (POST "/xlsx" [] web/xlsx)
           (route/not-found "<p>Page not found.</p>"))

(def app (-> app-routes wrap-params))

(defn shutdown-server []
  (timbre/info "Shutdown...")
  (state/stop-server)
  (browser/stop-phantom))

(defn start-server [options summary]
  (if (:help options) (exit 0 (server-usage summary)))
  (when (:log options)
    (init-logger (:log options)))
  (timbre/info (str "Starting export server on " (:host options) ":" (:port options)))
  (browser/setup-phantom)
  (state/set-server (run-server app {:port (:port options) :ip (:host options)}))
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-server)))


;====================================================================================
; Cmd Actions
;====================================================================================
(defn cmd-export [options summary]
  (if (:help options) (exit 0 (cmd-usage summary)))
  (let [script (:script options)
        file (:input-file options)]
    (cond
      (and (nil? script) (nil? file)) (exit 1 (error-msg ["script or file should be specified in 'cmd' mode."]))
      (and file (not (.exists (io/file file)))) (exit 1 (error-msg ["Input File not exists."]))
      :else (case (:type options)
              "png" (cmd/png options)
              "jpg" (cmd/jpg options)
              "svg" (cmd/svg options)
              "pdf" (cmd/pdf options)))))


;====================================================================================
; Main
;====================================================================================
(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args common-options)]
    (prn (:allow-scripts-executing options))
    (state/set-options options)
    (cond
      (:version options) (exit 0 server-name)
      (not= (count arguments) 1) (exit 1 (usage))
      errors (exit 1 (error-msg errors)))
    (reset! web/allow-script-executing (:allow-scripts-executing options))
    (timbre/info "Charset: " (Charset/defaultCharset))
    (timbre/info "FileEncoding: " (System/getProperty "file.encoding"))
    (case (first arguments)
      "server" (start-server options summary)
      "cmd" (cmd-export options summary)
      (exit 1 (usage)))))
