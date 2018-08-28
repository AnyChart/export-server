(ns export-server.browser.selenium.core
  (:require [export-server.browser.selenium.common :as common]
            [export-server.browser.selenium.svg-to-png :as svg-to-png-ns]
            [export-server.browser.selenium.script-to-png :as script-to-png-ns]
            [export-server.browser.selenium.html-to-png :as html-to-png-ns]))


(defn setup-drivers []
  (common/setup-drivers))

(defn stop-drivers []
  (common/stop-drivers))

(defn script-to-png [script exit options type]
  (script-to-png-ns/script-to-png script exit options type))

(defn svg-to-png [svg exit options]
  (svg-to-png-ns/svg-to-png svg exit options))

(defn html-to-png [file exit options & [svg-type?]]
  (html-to-png-ns/html-to-png file exit options svg-type?))