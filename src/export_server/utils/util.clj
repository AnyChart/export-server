(ns export-server.utils.util)

(defn jar-location
  "if run locally returns clojure.jar path"
  [& [ns]]
  (-> (or ns (class *ns*)) .getProtectionDomain .getCodeSource .getLocation .getPath clojure.java.io/file .getParent))