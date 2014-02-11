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
  Fixture setup, start LDAP server instance and bind to server atom
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

(def list-query-cli-args-for-test { :list-sites         '("GLUE Sites" 334)
                                    :list-services      '("GLUE Services" 2227)
                                    :list-clusters      '("GLUE Clusters" 510)
                                    :list-subclusters   '("GLUE Subclusters" 108)
                                    :list-ce            '("GLUE Computing Elements" 3170)
                                    :list-software      '("GLUE Software" 0)
                                    :list-se            '("GLUE Storage Elements" 437)
                                    :list-sa            '("GLUE Storage Areas" 1219)
                                    :list-vo            '("GLUE Virtual Organizations" 45)})

(deftest test-list-queries
  (doall
    (map
      #(testing (str "List " (first (% list-query-cli-args-for-test)) " - The test file contains " (second (% list-query-cli-args-for-test)))
        (is (= (second (% list-query-cli-args-for-test)) (count (gluequeries/glue-object-list-query (.getConnection @server) %)))))
      (keys list-query-cli-args-for-test))))

(deftest test-query-by-dn
  (testing "List Cyfronet Vo record"
    (is (= (list
             (list "objectClass" #{"Mds"})
             (list "dn" "Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid")
             (list "Mds-Vo-name" "CYFRONET-LCG2"))
          (gluequeries/glue-object-query (.getConnection @server) "Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid" :objectClass :dn :Mds-Vo-name)))))

(deftest test-query-by-foreign-key
  (testing "List Cyfronet Site relatives"
    (is (= (list
             "GlueClusterUniqueID=cream.grid.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueClusterUniqueID=cream02.grid.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=alice.grid.cyf-kr.edu.pl_VOBOX_3527526937,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=http://lfc.grid.cyf-kr.edu.pl:8085/,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=httpg://dpm.cyf-kr.edu.pl:8446/srm/managerv2,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=lfc.grid.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=local-http://lfc.grid.cyf-kr.edu.pl:8085/,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=local-lfc.grid.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=myproxy.grid.cyf-kr.edu.pl_MyProxy_3803520255,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=sbdii.grid.cyf-kr.edu.pl_bdii_site_3059482004,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueServiceUniqueID=zeus60.cyf-kr.edu.pl_bdii_top_1225164794,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueSEUniqueID=dpm.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid"
             "GlueSEUniqueID=storm.grid.cyf-kr.edu.pl,Mds-Vo-name=CYFRONET-LCG2,Mds-Vo-name=local,o=grid")
          (gluequeries/glue-object-elements (.getConnection @server) "GlueSiteUniqueID=CYFRONET-LCG2")))))



; TODO : add tests for other queries