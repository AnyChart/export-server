(ns export-server.state
  (:require [toml.core :as toml]
            [export-server.utils.config :as default-config]
            [clojure.walk :refer [keywordize-keys]]))

(defonce server (atom nil))
(defonce options (atom nil))

(defn set-server! [serv]
  (reset! server serv))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn set-options! [opts]
  (reset! options opts))

(defn combine-option [key options file-options default-config]
  (or (get options key) (get file-options key) (get default-config key)))

(defn combine-settings [mode
                        options
                        {cmd :cmd {{twitter :twitter :as sharing} :sharing images :images :as server} :server :as foptions}
                        config]
  {:mode                    (or mode (:mode foptions))

   ;; server options
   :port                    (combine-option :port options server config)
   :host                    (combine-option :host options server config)
   :log                     (combine-option :log options server config)
   :allow-scripts-executing (combine-option :allow-scripts-executing options server config)

   ;; images
   :saving-folder           (or (:saving-folder options) (:folder images) (:saving-folder config))
   :saving-url-prefix       (or (:saving-url-prefix options) (:prefix images) (:saving-url-prefix config))

   ;;sharing
   :sharing-port            (or (:sharing-port options) (:port sharing))
   :sharing-db              (or (:sharing-db options) (:db sharing))
   :sharing-user            (or (:sharing-user options) (:user sharing))
   :sharing-password        (or (:sharing-password options) (:password sharing))

   ;;twitter
   :twitter-key             (or (:twitter-key options) (:key twitter))
   :twitter-secret          (or (:twitter-secret options) (:secret twitter))
   :twitter-callback        (or (:twitter-callback options) (:callback twitter))

   ;; cmd
   :svg                     (combine-option :svg options cmd config)
   :svg-file                (combine-option :svg-file options cmd config)

   :output-file             (combine-option :output-file options cmd config)
   :container-width         (combine-option :container-width options cmd config)
   :pdf-height              (combine-option :pdf-height options cmd config)
   :image-width             (combine-option :image-width options cmd config)
   :output-path             (combine-option :output-path options cmd config)
   :container-height        (combine-option :container-height options cmd config)
   :force-transparent-white (combine-option :force-transparent-white options cmd config)
   :script                  (combine-option :script options cmd config)
   :config                  (combine-option :config options cmd config)
   :pdf-landscape           (combine-option :pdf-landscape options cmd config)
   :pdf-y                   (combine-option :pdf-y options cmd config)
   :type                    (combine-option :type options cmd config)
   :pdf-x                   (combine-option :pdf-x options cmd config)
   :image-height            (combine-option :image-height options cmd config)
   :pdf-size                (combine-option :pdf-size options cmd config)
   :pdf-width               (combine-option :pdf-width options cmd config)
   :container-id            (combine-option :container-id options cmd config)
   :input-file              (combine-option :input-file options cmd config)
   :jpg-quality             (combine-option :jpg-quality options cmd config)

   :help                    (:help options)})

(defn init [mode options]
  (if-let [config-path (:config options)]
    (when-let [file-options (try (-> config-path slurp (toml/read :keywordize))
                                 (catch Exception e nil))]
      (set-options! (combine-settings mode options file-options default-config/defaults)))
    (set-options! (combine-settings mode options {} default-config/defaults))))
