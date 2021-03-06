(ns gluebrowser.core-test
  (:require [clojure.test :refer :all]
            [gluebrowser.core :refer :all]
            [gluebrowser.gluequeries :as gluequeries])
  (:import (com.unboundid.ldap.listener  InMemoryDirectoryServerConfig
                                         InMemoryDirectoryServer)))


;
; LDAP server configuration
;
(defonce server (atom nil))

(defn ldap-config
  [dn user pass]
  (let [cfg (InMemoryDirectoryServerConfig. (into-array String ["Mds-Vo-name=local,o=grid"]))]
    (doto cfg
      ; setup servers user and password
      (.addAdditionalBindCredentials user pass)
      ; disable schema checking
      (.setSchema nil))) )



(defn setup
  "
  Fixture setUp, start LDAP server instance and bind to server atom
  "
  []
  (reset! server (InMemoryDirectoryServer. (ldap-config "Mds-Vo-name=local,o=grid"
                                                        "cn=admin, Mds-Vo-name=local, o=grid"
                                                        "alamakota")))
  (doto @server  (.importFromLDIF false "./resources/glue-test.ldif") (.startListening)))


(defn teardown
  "
  Fixture teardown, stop LDAP server instance and bind to server atom
  "
  []
  (.shutDown @server true))


;
; Wrapper for setUp and tearDown methods
;
(defn test-wrapper
  [f]
  (do
    (println "Unit test wrapper for setup/teardown")
    (setup)
    (f)
    (teardown)))


;
; Use the setup wrappers once for the entire namespace
;
(use-fixtures :once test-wrapper)



(deftest test-list-sites
  (testing "List Glue Sites - The test file contains 293"
    (is (= 293 (count (gluequeries/glue-object-list-query (.getConnection @server) :list-sites ))))))

(deftest test-list-se
  (testing "List Glue SE - The test file contains 282"
    (is (= 282 (count (gluequeries/glue-object-list-query (.getConnection @server) :list-se ))))))

; TODO : add tests for other queries
