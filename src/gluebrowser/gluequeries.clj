(ns gluebrowser.gluequeries
  (:use [clojure.string :only [join]])
  (:require [clj-ldap.client :as ldap])
  (:use [clojure.tools.logging :only [info error]]))


(def TODO nil)

;
; The default root DN
;
(def ldap-root-dn "Mds-Vo-name=local,o=grid")

;
; Definition of LDAP queries compatible with ldap-clj format
; for retrieving list of GLUE objects by specific type and
; limited to certain attributes
;
(def glue-list-queries {  :list-sites         '("GlueSite"        :GlueSiteUniqueID)
                          :list-clusters      '("GlueCluster"     :GlueClusterUniqueID)
                          :list-subclusters   '("GlueSubcluster"  :GlueSubClusterUniqueID)
                          :list-ce            '("GlueCE"          :GlueCEUniqueID)
                          :list-software      '("GlueSoftware"    :GlueSoftware) ;TODO: wrong, find correct names
                          :list-se            '("GlueSE"          :GlueSEUniqueID)
                          :list-sa            '("GlueSA"          :GlueSALocalID)
                          :list-vo            '("GlueVOInfo"      :GlueVOInfoLocalID)
                          :list-services      '("GlueService"     :GlueServiceUniqueID)})


(defn glue-object-list-query
  "
  Queries the LDAP for IDs of objects with specific type (LDAP's objectClass)

  For instance for object class  GlueSite the list should contain only the
  attribute GlueSiteUniqueID of all objects returned by proper LDAP query, e.g.
  (\"ARNES\" \"TRIUMF_LCG2\" ...)
  "
  [ldap-server query]
  (let [[object-class ldap-attribute] (query glue-list-queries)
        ldap-query (str "(objectClass=" object-class ")")]
    (info "Executing list query: " ldap-query " for attribute " ldap-attribute)
    (for [result (ldap/search ldap-server ldap-root-dn {:filter ldap-query
                                                        :attributes [ldap-attribute]})]
      [(ldap-attribute result) (:dn result)])))
;{:filter "(objectClass=GlueSite)" :attributes [:GlueSiteUniqueID]}

(defn glue-object-query
  "
  Returns a GLUE object by ID with optionally limited set of attributes. If attrs is nil
  then it returns a map with all object's attributes returned by LDAP query.
  "
  [ldap-server id & attrs]
  (do
    (info (str "Executing object ID query for id: " id (if (not-empty attrs) (str " with attributes: " (join ", " attrs)) "") "."))
    (when-let [query-result (try
                              (if (empty? attrs)
                                (ldap/get ldap-server id)
                                (ldap/get ldap-server id attrs))
                              (catch Exception e nil))]
      (for [key (keys query-result)]
        [(name key) (key query-result)]))))


(defn glue-object-elements
  "
  Returns a list of GLUE objects referenced by object with 'id' of specific type.
  In GLUE this kind of semantics is achieved through GlueForeignKey attribute, for instance
  an object of type GlueCluster will be related to a GlueSite in which it is deployed
  through such entry:

  - GlueForeignKey: GlueSiteUniqueID=ARNES

  "
  [ldap-server id & types]
  (do
    (info (str "Executing foreign key query for id: " id (if (not= (count types) 0) (str " limited to type(s): " (join ", " types)) "") "."))
    (map :dn (ldap/search
               ldap-server
               ldap-root-dn
               {:filter (if (empty? types)
                          (str "(GlueForeignKey=" id ")")
                          (if (= 1 (count types))
                            (str "(&(objectClass=" (first types) ")(GlueForeignKey=" id "))")
                            (str "(&(|(objectClass=" (join ")(objectClass=" types) "))(GlueForeignKey=" id "))")))
                :attributes [:dn]}))))
