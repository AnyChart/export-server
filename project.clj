(defproject export-server "1.2.2"
  :description "AnyChart export server, AnyChart Bundle version 7.10.1"
  :url "https://github.com/AnyChart/export-server"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Dphantomjs.binary.path=/usr/local/bin/phantomjs"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;http
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [http-kit "2.1.19"]
                 [compojure "1.5.0"]
                 [ring "1.5.0"]

                 ;phantomJS
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 [clj-webdriver/clj-webdriver "0.7.2"]
                 [com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]

                 ;params validation
                 [bouncer "1.0.0"]

                 ;command line args
                 [org.clojure/tools.cli "0.3.5"]

                 ;rasterization
                 [clj-pdf "2.2.1"]
                 [tikkba "0.6.0" :exclusions [org.clojure/clojure org.clojars.pallix/batik]]
                 [org.clojars.pallix/xerces "2.5.0"]
                 [org.clojars.pallix/xml-apis "2.5.0"]
                 [org.clojars.pallix/xml-apis-ext "2.5.0"]

                 [org.apache.xmlgraphics/batik-transcoder "1.8"]
                 [org.apache.xmlgraphics/batik-xml "1.8"]
                 [org.apache.xmlgraphics/batik-codec "1.8"]

                 ;csv to xslx
                 [dk.ative/docjure "1.10.0"]
                 [org.clojure/data.csv "0.1.3"]

                 ;JSON
                 [cheshire "5.6.1"]

                 ;; logging
                 [com.taoensso/timbre "4.4.0"]

                 ;tests
                 [peridot "0.4.4"]]
  :plugins [[lein-localrepo "0.5.3"]
            [lein-ancient "0.6.10"]]
  :main ^:aot export-server.core
  :profiles {:uberjar {:aot :all}}
  :jar-name "export-server.jar"
  :uberjar-name "export-server-standalone.jar"
  )
