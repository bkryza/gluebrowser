(ns gluebrowser.gluequeries
  (:use [clojure.string :only (join)])
  (:require [clj-ldap.client :as ldap])
  (:use [clojure.tools.logging :only (info error)]))


(def TODO nil)

;
; The default root DN
;
(def ldap-root-dn "Mds-Vo-name=local,o=grid")

;
; Definition of LDAP queries compatible with ldap-clj format
; for retrieving list of GLUE objects by specific type and
; limited to certain attributes }
;
(def glue-queries
  {
    :list-sites '("GlueSite" :GlueSiteUniqueID) ;{:filter "(objectClass=GlueSite)" :attributes [:GlueSiteUniqueID]}
    :list-clusters '("GlueClusters" :GlueClusterUniqueID)
    :list-subclusters '("GlueSubclusters" :GlueSubclusterUniqueID)
    :list-ce '("GlueCE" :GlueCEUniqueID)
    :list-software '("GlueSoftware" :GlueSoftware);check
    :list-se '("GlueSE" :GlueSEUniqueID)
    :list-sa '("GlueSA" :GlueSAUniqueID)
    :list-vo '("GlueVO" :GlueVO)
    :list-services '("GlueServices" :GlueServicesUniqueID)
    }
  )


(defn glue-object-list-query
  "
  Queries the LDAP for IDs of objects with specific type (LDAP's objectClass)

  For instance for object class  GlueSite the list should contain only the
  attribute GlueSiteUniqueID of all objects returned by proper LDAP query, e.g.
  (\"ARNES\" \"TRIUMF_LCG2\" ...)
  "
  [ldap-server query]
  (let [ldap-query (apply str "(objectClass=" (first (query glue-queries)) ")")
        ldap-attribute (second (query glue-queries))]
    (info "Executing list query: " ldap-query " for attribute " ldap-attribute)
    (let [result (ldap/search ldap-server ldap-root-dn {:filter ldap-query :attributes [ldap-attribute]})]
      (map ldap-attribute result))))



(defn glue-object-query
  "
  Returns a GLUE object by ID with optionally limited set of attributes. If attrs is nil
  then it returns a map with all object's attributes returned by LDAP query.
  "
  [ldap-server id & attrs]
  TODO)



(defn glue-object-elements
  "
  Returns a list of GLUE objects referenced by object with 'id' of specific type.
  In GLUE this kind of semantics is achieved through GlueForeignKey attribute, for instance
  an object of type GlueCluster will be related to a GlueSite in which it is deployed
  through such entry:

  - GlueForeignKey: GlueSiteUniqueID=ARNES

  "
  [ldap-server id type]
  TODO)

