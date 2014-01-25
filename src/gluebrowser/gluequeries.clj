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
; limited to certain attributes
;
(def glue-list-queries
  {
    :list-sites '("GlueSite" :GlueSiteUniqueID) ;{:filter "(objectClass=GlueSite)" :attributes [:GlueSiteUniqueID]}
    :list-clusters '("GlueClusters" :GlueClusterUniqueID)
    :list-subclusters '("GlueSubclusters" :GlueSubclusterUniqueID)
    :list-ce '("GlueCE" :GlueCEUniqueID)
    :list-software '("GlueSoftware" :GlueSoftware);wrong
    :list-se '("GlueSE" :GlueSEUniqueID)
    :list-sa '("GlueSA" :GlueSAUniqueID);wrong
    :list-vo '("GlueVO" :GlueVOViewLocalID);??
    :list-services '("GlueServices" :GlueServicesUniqueID)
    })


(defn glue-object-list-query
  "
  Queries the LDAP for IDs of objects with specific type (LDAP's objectClass)

  For instance for object class  GlueSite the list should contain only the
  attribute GlueSiteUniqueID of all objects returned by proper LDAP query, e.g.
  (\"ARNES\" \"TRIUMF_LCG2\" ...)
  "
  [ldap-server query]
  (let [ldap-query (apply str "(objectClass=" (first (query glue-list-queries)) ")")
        ldap-attribute (second (query glue-list-queries))]
    (info "Executing list query: " ldap-query " for attribute " ldap-attribute)
    (let [result (ldap/search ldap-server ldap-root-dn {:filter ldap-query :attributes [ldap-attribute]})]
      (map ldap-attribute result))))

;(defn get-object-by-id
;  "
;  Returns pure LDAP oobject by it's ID
;  "
;  [ldap-server id]
;  (first (filter (complement nil?) (map
;    #(try (ldap/get ldap-server (str (name (second (% glue-list-queries))) "=" id)) (catch Exception e nil))
;    (keys glue-list-queries)))))
;
;(defn glue-object-query
;  "
;  Returns a GLUE object by ID with optionally limited set of attributes. If attrs is nil
;  then it returns a map with all object's attributes returned by LDAP query.
;  "
;  [ldap-server id & attrs]
;  (do
;    (info (str "Executing object ID query for id: " id (if (not= (count attrs) 0) (str " with attributes: " (join " " attrs)) "") "."))
;    (if-let [query-result (get-object-by-id ldap-server id)]
;      (if (= (count attrs) 0)
;        query-result
;        (apply merge (map #(hash-map %1 (%1 query-result)) attrs)))
;      (apply merge (map #(hash-map %1 nil) attrs)))
;    ))

(defn glue-object-query
  "
  Returns a GLUE object by ID with optionally limited set of attributes. If attrs is nil
  then it returns a map with all object's attributes returned by LDAP query.
  "
  [ldap-server id & attrs]
  (do
    (info (str "Executing object ID query for id: " id (if (not= (count attrs) 0) (str " with attributes: " (join " " attrs)) "") "."))
    (try (if (= (count attrs) 0) (ldap/get ldap-server id) (ldap/get ldap-server id attrs)) (catch Exception e nil))))

;(defn glue-object-query
;  "
;  Returns a GLUE object by ID with optionally limited set of attributes. If attrs is nil
;  then it returns a map with all object's attributes returned by LDAP query.
;  "
;  [ldap-server id & attrs]
;  (do
;  (info (str "Executing object ID query for id: " id (if (not= (count attrs) 0) (" with attributes: " (join " " attrs)) "") "."))
;  (loop [unprocessed-basic-queries (keys glue-basic-queries)]
;    (if (not= (count unprocessed-basic-queries) 0)
;      (if-let
;        [ldap-result
;         (try (ldap/get ldap-server (str (name (second ((first unprocessed-basic-queries) glue-basic-queries))) "=" id)) (catch Exception e ))]
;        ldap-result
;        (recur (rest unprocessed-basic-queries)))
;      {}))))

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

