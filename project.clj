(defproject export-server "1.6.0"
  :description "AnyChart export server, AnyChart Bundle version 8.10.0"
  :url "https://github.com/AnyChart/export-server"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Dphantomjs.binary.path=/usr/local/bin/phantomjs"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;http
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]
                 [ring "1.6.3"]

                 ;; sharing
                 [selmer "1.12.0"]
                 [oauth/oauth.one "0.7.0"]
                 [ring/ring-codec "1.1.1"]
                 [clj-http "2.3.0"]
                 [camel-snake-kebab "0.4.0"]

                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [honeysql "0.8.1"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [mysql/mysql-connector-java "6.0.4"]
                 [clojurewerkz/urly "1.0.0"]

                 ;phantomJS
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 ;[clj-webdriver "0.7.2"]
                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
                 ;[org.seleniumhq.selenium/selenium-java "3.3.1"
                 ; :exclusions [org.seleniumhq.selenium/selenium-support]]
                 ;[com.codeborne/phantomjsdriver "1.4.2"
                 ; :exclusions [org.seleniumhq.selenium/selenium-java
                 ;              org.seleniumhq.selenium/selenium-server
                 ;              org.seleniumhq.selenium/selenium-remote-driver]]
                 ;;; https://mvnrepository.com/artifact/com.google.guava/guava

                 [com.google.guava/guava "23.0"]
                 ;[com.codeborne/phantomjsdriver "1.4.2"]
                 ;[com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]
                 ;; https://mvnrepository.com/artifact/com.codeborne/phantomjsdriver
                 [com.codeborne/phantomjsdriver "1.4.4"]

                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-api
                 [org.seleniumhq.selenium/selenium-api "3.11.0"]
                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-server
                 [org.seleniumhq.selenium/selenium-server "3.11.0"]
                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-remote-driver
                 [org.seleniumhq.selenium/selenium-remote-driver "3.11.0"]
                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
                 [org.seleniumhq.selenium/selenium-java "3.11.0"]
                 ;; https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
                 [org.seleniumhq.selenium/selenium-firefox-driver "3.11.0"]

                 [etaoin "0.2.8"]
                 [tupelo "0.9.83"]

                 ;[selmer "1.0.7"]
                 ;jbrowser deps, need to comment clj-wedriver and phantomjsdriver
                 ;[com.machinepublishers/jbrowserdriver "0.14.12"]
                 ;[org.slf4j/slf4j-api "1.7.21"]
                 ;[org.slf4j/slf4j-simple "1.7.21"]

                 ;params validation
                 [bouncer "1.0.1"]

                 ;command line args
                 [org.clojure/tools.cli "0.3.7"]
                 [toml "0.1.3"]
                 [org.apache.commons/commons-lang3 "3.7"]

                 ;rasterization
                 [clj-pdf "2.2.33"]
                 [tikkba "0.6.0" :exclusions [org.clojure/clojure org.clojars.pallix/batik]]
                 [org.clojars.pallix/xerces "2.5.0"]
                 [org.clojars.pallix/xml-apis "2.5.0"]
                 [org.clojars.pallix/xml-apis-ext "2.5.0"]

                 [org.apache.xmlgraphics/batik-transcoder "1.9.1"]
                 [org.apache.xmlgraphics/batik-xml "1.9.1"]
                 [org.apache.xmlgraphics/batik-codec "1.9.1"]

                 ;csv to xslx
                 [dk.ative/docjure "1.10.0"]
                 [org.clojure/data.csv "0.1.4"]

                 ;JSON
                 [cheshire "5.8.0"]

                 ;; logging and other utils
                 [com.taoensso/timbre "4.10.0"]
                 [clj-time "0.14.4"]
                 [digest "1.4.8"]
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
