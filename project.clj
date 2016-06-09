(defproject export-server "1.2.2"
  :description "AnyChart export server, AnyChart Bundle version 7.10.1"
  :url "https://github.com/AnyChart/export-server"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Dphantomjs.binary.path=/usr/local/bin/phantomjs"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ;http
                 [http-kit "2.1.13"]
                 [compojure "1.1.6"]
                 [ring "1.2.2"]

                 ;phantomJS
                 [org.apache.httpcomponents/httpclient "4.3.1"]
                 [clj-webdriver/clj-webdriver "0.6.0"]
                 [com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]

                 ;params validation
                 [bouncer "0.3.2"]

                 ;command line args
                 [org.clojure/tools.cli "0.3.1"]

                 ;rasterization
                 [clj-pdf "2.2.0"]
                 [tikkba "0.5.0" :exclusions [org.clojure/clojure org.clojars.pallix/batik]]
                 [org.clojars.pallix/xerces "2.5.0"]
                 [org.clojars.pallix/xml-apis "2.5.0"]
                 [org.clojars.pallix/xml-apis-ext "2.5.0"]

                 [org.apache.xmlgraphics/batik-transcoder "1.8"]
                 [org.apache.xmlgraphics/batik-xml "1.8"]
                 [org.apache.xmlgraphics/batik-codec "1.7"]

                 ;csv to xslx
                 [dk.ative/docjure "1.10.0"]
                 [org.clojure/data.csv "0.1.3"]

                 ;JSON
                 [cheshire "5.3.1"]

                 ;tests
                 [peridot "0.2.2"]]
  :plugins [[lein-localrepo "0.5.3"]]
  :main ^:aot export-server.core
  :profiles {:uberjar {:aot :all}}
  :jar-name "export-server.jar"
  :uberjar-name "export-server-standalone.jar"
  )
