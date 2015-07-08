(ns export-server.web-handlers-test
  (:use [clojure.test]
        [export-server.test-utils]))

;====================================================================================
; /png
;====================================================================================
(deftest png-handler-test
  (let [response ((call-post "/png" {}) :response)]
    (testing "Empty params"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/png" {:data-type "json" :response-type "base64"}) :response)]
    (testing "No data param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/png" {:data correct-svg-string :response-type "base64"}) :response)]
    (testing "No dataType param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))


  (let [response ((call-post "/png" {:data correct-svg-string :data-type "json"}) :response)]
    (testing "No :response-type param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/png" {:data correct-svg-string :data-type "svg" :response-type "base64"}) :response)]
    (testing "Success svg --> base64 image request"
      (is (= (response :status) 200))
      (is (= (response :body) (str "{\"result\":\"" correct-svg-to-base64-png-string "\"}")))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/png" {:data correct-script-string :data-type "script" :response-type "base64"}) :response)]
    (testing "Success sctipt --> base64 image request"
      (is (= (response :status) 200))
      (is (= (response :body) (str "{\"result\":\"" correct-script-to-base64-png-string "\"}")))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/png" {:data correct-svg-string :data-type "svg" :response-type "file"}) :response)]
    (testing "Success svg --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/png"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/png" {:data correct-script-string :data-type "script" :response-type "file"}) :response)]
    (testing "Success script --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/png"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/png" {:data correct-script-string :dataType "script" :responseType "file"}) :response)]
    (testing "Legacy test"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/png"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary")))))

;====================================================================================
; /jpg
;====================================================================================
(deftest jpg-handler-test
  (let [response ((call-post "/jpg" {}) :response)]
    (testing "Empty params"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/jpg" {:data-type "json" :response-type "base64"}) :response)]
    (testing "No data param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/jpg" {:data correct-svg-string :response-type "base64"}) :response)]
    (testing "No dataType param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))


  (let [response ((call-post "/jpg" {:data correct-svg-string :data-type "json"}) :response)]
    (testing "No :response-type param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/jpg" {:data correct-svg-string :data-type "svg" :response-type "base64"}) :response)]
    (testing "Success svg --> base64 image request"
      (is (= (response :status) 200))
      (is (= (response :body) (str "{\"result\":\"" correct-svg-to-base64-jpg-string "\"}")))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/jpg" {:data correct-script-string :data-type "script" :response-type "base64"}) :response)]
    (testing "Success sctipt --> base64 image request"
      (is (= (response :status) 200))
      (is (= (response :body) (str "{\"result\":\"" correct-script-to-base64-jpg-string "\"}")))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/jpg" {:data correct-svg-string :data-type "svg" :response-type "file"}) :response)]
    (testing "Success svg --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/jpeg"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/jpg" {:data correct-script-string :data-type "script" :response-type "file"}) :response)]
    (testing "Success script --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/jpeg"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/jpg" {:data correct-script-string :dataType "script" :responseType "file"}) :response)]
    (testing "Legacy test"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/jpeg"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary")))))

;====================================================================================
; /pdf
;====================================================================================
(deftest pdf-handler-test
  (let [response ((call-post "/pdf" {}) :response)]
    (testing "Empty params"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/pdf" {:data-type "json" :response-type "base64"}) :response)]
    (testing "No data param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/pdf" {:data correct-svg-string :response-type "base64"}) :response)]
    (testing "No dataType param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))


  (let [response ((call-post "/pdf" {:data correct-svg-string :data-type "json"}) :response)]
    (testing "No :response-type param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/pdf" {:data correct-svg-string :data-type "svg" :response-type "base64"}) :response)]
    (testing "Success svg --> base64 image request"
      (is (= (response :status) 200))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/pdf" {:data correct-script-string :data-type "script" :response-type "base64"}) :response)]
    (testing "Success sctipt --> base64 image request"
      (is (= (response :status) 200))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/pdf" {:data correct-svg-string :data-type "svg" :response-type "file"}) :response)]
    (testing "Success svg --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "application/pdf"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/pdf" {:data correct-script-string :data-type "script" :response-type "file"}) :response)]
    (testing "Success script --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "application/pdf"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/pdf" {:data correct-script-string :dataType "script" :responseType "file"}) :response)]
    (testing "Legacy test"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "application/pdf"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary")))))

;====================================================================================
; /svg
;====================================================================================
(deftest svg-handler-test
  (let [response ((call-post "/svg" {}) :response)]
    (testing "Empty params"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/svg" {:data-type "json" :response-type "base64"}) :response)]
    (testing "No data param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/svg" {:data correct-svg-string :response-type "base64"}) :response)]
    (testing "No dataType param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))


  (let [response ((call-post "/svg" {:data correct-svg-string :data-type "json"}) :response)]
    (testing "No :response-type param"
      (is (= (response :status) 400))
      (is (> (.indexOf (response :body) "error") 0))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/svg" {:data correct-svg-string :data-type "svg" :response-type "base64"}) :response)]
    (testing "Success svg --> base64 image request"
      (is (= (response :status) 200))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/svg" {:data correct-script-string :data-type "script" :response-type "base64"}) :response)]
    (testing "Success sctipt --> base64 image request"
      (is (= (response :status) 200))
      (is (= ((response :headers) "Content-Type") "json"))))

  (let [response ((call-post "/svg" {:data correct-svg-string :data-type "svg" :response-type "file"}) :response)]
    (testing "Success svg --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/svg+xml"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/svg" {:data correct-script-string :data-type "script" :response-type "file"}) :response)]
    (testing "Success script --> file image request"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/svg+xml"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary"))))

  (let [response ((call-post "/svg" {:data correct-script-string :dataType "script" :responseType "file"}) :response)]
    (testing "Legacy test"
      (is (= (response :status) 200))
      (is (instance? java.io.File (response :body)))
      (is (= ((response :headers) "Content-Type") "image/svg+xml"))
      (is (= ((response :headers) "Content-Description") "File Transfer"))
      (is (= ((response :headers) "Content-Transfer-Encoding") "binary")))))

;====================================================================================
; /status
;====================================================================================
(deftest status-handler-test
  (let [response ((call-post "/status" {}) :response)]
    (testing "POST status request"
      (is (= (response :status) 200))
      (is (= (response :body) "ok"))))

  (let [response ((call-get "/status" {}) :response)]
    (testing "POST status request"
      (is (= (response :status) 200))
      (is (= (response :body) "ok")))))
