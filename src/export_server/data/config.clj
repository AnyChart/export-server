(ns export-server.data.config
  (:require [export-server.utils.util :refer [jar-location]]))

;; sizes in millimeters
(def available-pdf-sizes {:a0           {:width 841 :height 1189}
                          :a1           {:width 594 :height 841}
                          :a2           {:width 420 :height 594}
                          :a3           {:width 297 :height 420}
                          :a4           {:width 210 :height 297}
                          :a5           {:width 148 :height 210}
                          :a6           {:width 105 :height 148}
                          :a7           {:width 74 :height 105}
                          :a8           {:width 52 :height 74}
                          :a9           {:width 37 :height 52}
                          :a10          {:width 26 :height 37}
                          :b0           {:width 1000 :height 1414}
                          :b1           {:width 707 :height 1000}
                          :b2           {:width 500 :height 707}
                          :b3           {:width 353 :height 500}
                          :b4           {:width 250 :height 353}
                          :b5           {:width 176 :height 250}
                          :b6           {:width 125 :height 176}
                          :b7           {:width 88 :height 125}
                          :b8           {:width 62 :height 88}
                          :b9           {:width 44 :height 62}
                          :b10          {:width 31 :height 44}
                          :arch-a       {:width 228.6 :height 304.8}
                          :arch-b       {:width 304.8 :height 457.2}
                          :arch-c       {:width 457.2 :height 609.6}
                          :arch-d       {:width 609.6 :height 914.4}
                          :arch-e       {:width 914.4 :height 1219.2}
                          :crown-octavo {:width 190 :height 126}
                          :crown-quarto {:width 190 :height 250}
                          :demy-octavo  {:width 221 :height 142}
                          :demy-quarto  {:width 220 :height 285}
                          :royal-octavo {:width 253 :height 158}
                          :royal-quarto {:width 253 :height 316}
                          :executive    {:width 184.15 :height 266.7}
                          :halfletter   {:width 140 :height 216}
                          :ledger       {:width 432 :height 279}
                          :legal        {:width 216 :height 356}
                          :letter       {:width 216 :height 279}
                          :tabloid      {:width 279 :height 432}})


(defn mm-to-pixel [mm]
  (int (* 3.779527559 mm)))


(def available-rasterization-data-types #{"script" "svg"})


(def available-rasterization-response-types #{"file" "base64"})


(def defaults {
               :engine                  :phantom

               ;; server
               :allow-scripts-executing false
               :port                    2000
               :host                    "localhost"
               :log                     nil
               :saving-url-prefix       ""
               :saving-folder           (str (jar-location) "/save")

               ;; cmd
               :output-file             "anychart"
               :output-path             ""
               :container-width         "100%"
               :container-height        "100%"
               :container-id            "container"
               :type                    "png"
               :data-type               "svg"
               :image-width             1024
               :image-height            800
               :force-transparent-white false
               :jpg-quality             1
               :pdf-size                nil                 ;:a4
               :pdf-x                   0
               :pdf-y                   0
               :pdf-width               595                 ;nil
               :pdf-height              842                 ;nil
               :pdf-landscape           false})


(defn min-size [image-size container-size]
  (let [container-size-int (if (number? container-size)
                             container-size
                             (try (Integer/parseInt container-size)
                                  (catch Exception _ nil)))]
    (if (some? container-size-int)
      (min image-size container-size-int)
      container-size)))