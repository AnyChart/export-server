(ns export-server.utils.config)

(def available-pdf-sizes {:a0           {:width "841mm" :height "1189mm"}
                          :a1           {:width "594mm" :height "841mm"}
                          :a2           {:width "420mm" :height "594mm"}
                          :a3           {:width "297mm" :height "420mm"}
                          :a4           {:width "210mm" :height "297mm"}
                          :a5           {:width "148mm" :height "210mm"}
                          :a6           {:width "105mm" :height "148mm"}
                          :a7           {:width "74mm" :height "105mm"}
                          :a8           {:width "52mm" :height "74mm"}
                          :a9           {:width "37mm" :height "52mm"}
                          :a10          {:width "26mm" :height "37mm"}
                          :b0           {:width "1000mm" :height "1414mm"}
                          :b1           {:width "707mm" :height "1000mm"}
                          :b2           {:width "500mm" :height "707mm"}
                          :b3           {:width "353mm" :height "500mm"}
                          :b4           {:width "250mm" :height "353mm"}
                          :b5           {:width "176mm" :height "250mm"}
                          :b6           {:width "125mm" :height "176mm"}
                          :b7           {:width "88mm" :height "125mm"}
                          :b8           {:width "62mm" :height "88mm"}
                          :b9           {:width "44mm" :height "62mm"}
                          :b10          {:width "31mm" :height "44mm"}
                          :arch-a       {:width "228.6mm" :height "304.8mm"}
                          :arch-b       {:width "304.8mm" :height "457.2mm"}
                          :arch-c       {:width "457.2mm" :height "609.6mm"}
                          :arch-d       {:width "609.6mm" :height "914.4mm"}
                          :arch-e       {:width "914.4mm" :height "1219.2mm"}
                          :crown-octavo {:width "190mm" :height "126mm"}
                          :crown-quarto {:width "190mm" :height "250mm"}
                          :demy-octavo  {:width "221mm" :height "142mm"}
                          :demy-quarto  {:width "220mm" :height "285mm"}
                          :royal-octavo {:width "253mm" :height "158mm"}
                          :royal-quarto {:width "253mm" :height "316mm"}
                          :executive    {:width "184.15mm" :height "266.7mm"}
                          :halfletter   {:width "140mm" :height "216mm"}
                          :ledger       {:width "432mm" :height "279mm"}
                          :legal        {:width "216mm" :height "356mm"}
                          :letter       {:width "216mm" :height "279mm"}
                          :tabloid      {:width "279mm" :height "432mm"}
                          })

(def available-rasterization-data-types #{"script" "svg"})

(def available-rasterization-response-types #{"file" "base64"})

(def defaults {
               :port 2000
               :host "localhost"
               :log "log.txt"
               :container-width  "1024px"
               :container-height "800px"
               :container-id "container"
               :type "png"
               :data-type "svg"
               :image-width 1024
               :image-height 800
               :force-transparent-white false
               :jpg-quality 1
               :pdf-size :a4
               :pdf-x 0
               :pdf-y 0
               :pdf-width nil
               :pdf-height nil
               :pdf-landscape false
               })
