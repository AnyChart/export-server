(ns export-server.browser.etaoin.core
  (:require [export-server.browser.etaoin.common :as common]
            [export-server.browser.etaoin.script-to-png :as script-to-png-ns]
            [export-server.browser.etaoin.svg-to-png :as svg-to-png-ns]
            [export-server.browser.etaoin.html-to-png :as html-to-png-ns]))


(defn setup-drivers []
  (common/setup-drivers))

(defn stop-drivers []
  (common/stop-drivers))

(defn script-to-png [script quit-ph exit-on-error options type]
  (script-to-png-ns/script-to-png script quit-ph exit-on-error options type))

(defn svg-to-png [svg quit-ph exit-on-error options]
  (svg-to-png-ns/svg-to-png svg quit-ph exit-on-error options))

(defn html-to-png [file quit-ph exit-on-error options & [svg-type?]]
  (html-to-png-ns/html-to-png file quit-ph exit-on-error options svg-type?))