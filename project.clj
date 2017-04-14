(defproject export-server "1.3.2"
  :description "AnyChart export server, AnyChart Bundle version 7.12.0"
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

                 ;; sharing
                 [selmer "1.10.1"]
                 [oauth/oauth.one "0.7.0"]
                 [ring/ring-codec "1.0.1"]
                 [clj-http "2.3.0"]
                 [camel-snake-kebab "0.4.0"]

                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]
                 [honeysql "0.8.1"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [mysql/mysql-connector-java "6.0.4"]
                 [clojurewerkz/urly "1.0.0"]

                 ;phantomJS
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 [clj-webdriver/clj-webdriver "0.7.2"]
                 [com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]
                 ;[selmer "1.0.7"]
                 ;jbrowser deps, need to comment clj-wedriver and phantomjsdriver
                 ;[com.machinepublishers/jbrowserdriver "0.14.12"]
                 ;[org.slf4j/slf4j-api "1.7.21"]
                 ;[org.slf4j/slf4j-simple "1.7.21"]

                 ;params validation
                 [bouncer "1.0.0"]

                 ;command line args
                 [org.clojure/tools.cli "0.3.5"]
                 [toml "0.1.2"]

                 ;rasterization
                 [clj-pdf "2.2.1"]
                 [tikkba "0.6.0" :exclusions [org.clojure/clojure org.clojars.pallix/batik]]
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
                 [cheshire "5.6.1"]

                 ;; logging and other utils
                 [com.taoensso/timbre "4.4.0"]
                 [clj-time "0.12.0"]
                 [digest "1.4.4"]
                 [me.raynes/fs "1.4.6"]

                 ;tests
                 [peridot "0.4.4"]]
  :plugins [[lein-localrepo "0.5.3"]
            [lein-ancient "0.6.10"]]
  :main ^:aot export-server.core
  :profiles {:dev {:jvm-opts ["-Ddev=true"]}
             :uberjar {:aot :all}}
  :jar-name "export-server.jar"
  :uberjar-name "export-server-standalone.jar")
