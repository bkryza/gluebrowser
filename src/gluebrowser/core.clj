(ns gluebrowser.core
  (:use [clojure.tools.cli :only (cli)])
  (:use [clojure.string :only (join)])
  (:require [clj-ldap.client :as ldap])
  (:require [gluebrowser.gluequeries :as gluequeries])
  (:use [clojure.tools.logging :only (info error)] )
  (:gen-class :main true))

(defn project-description
  "
  Returns textual project description header
  "
  []
  "
  --------------------------------------------------
  GLUE LDAP Browser implemented in Clojure
  --------------------------------------------------

  ")

(def TODO nil)


(defn query-server
  "
  Connects to the ldap server and executes the query on it passing it any argument list
  "
  ; why & args if there always is only one???
  [connection-data query-fn & args]
  (try
    (let [url (join ":" [(:host connection-data) (:port connection-data)])
          ldap-server (ldap/connect {:host     url
                                     :bind-dn  (:dn connection-data)
                                     :password (:password connection-data)})]
      ;
      ; Execute the query on the ldap-server
      ;
      (info "Querying LDAP")
      (apply query-fn (cons ldap-server args)))
    (catch Exception e
      (println "Couldn't connect to LDAP server: " (.getMessage e)))))



;;(defn print-glue-result
;;  "
;;
;;  "
;;  )


;
; Set of all command line attributes which handle list queries
;
(def list-query-cli-args #{:list-sites
                           :list-services
                           :list-clusters
                           :list-subclusters
                           :list-ce
                           :list-software
                           :list-se
                           :list-sa
                           :list-vo})



(defn dispatch-cli-queries
  "
  This method executes all queries passed on the command line
  "
  [opts]
  (let [connection-data {:host (:host opts) :port (:port opts) :dn (:dn opts) :password (:password opts)}]
    (do
      ;
      ; TODO: loop over all command line query arguments
      ;
      ; DONE: Refactor query-server method so that the connection parameters host, port, dn, password
      ; can be passed in a single dictionary
      ;

      (if (:list-sites opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-sites))
      (if (:list-services opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-services ))
      (if (:list-clusters opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-clusters))
      (if (:list-subclusters opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-subclusters))
      (if (:list-ce opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-ce))
      (if (:list-software opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-software))
      (if (:list-se opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-se))
      (if (:list-sa opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-sa))
      (if (:list-vo opts) (query-server connection-data
                            gluequeries/glue-object-list-query
                            :list-vo))



      ;(loop [available-options list-query-cli-args acc '()]
      ;  (if ((first available-options) opts)
      ;    (recur (rest available-options) (cons (query-server connection-data gluequeries/glue-object-list-query (first available-options)) acc))
      ;    (if (= available-options #[]) (recur (rest available-options) acc)
      ;       )))
      ;




      ; Handle other list-* options
      ;
      ;
      )))


(defn -main
  "The application's main function"
  [& args]
  (let [[opts args banner]
        (cli args
          ["-h" "--help" "Show help" :flag true :default false]
          ["-s" "--host" "GLUE LDAP host" :default "127.0.0.1"]
          ["-p" "--port" "GLUE LDAP port" :default 389]
          ["-d" "--dn" "Authentication user DN" ]
          ["-w" "--password" "Authentication password"]
          ["-ls" "--list-sites" "List GLUE Sites" :flag true :default false]
          ["-lv" "--list-services" "List GLUE Services" :flag true :default false]
          ["-lc" "--list-clusters" "List GLUE Clusters" :flag true :default false]
          ["-lsc" "--list-subclusters" "List GLUE Subclusters" :flag true :default false]
          ["-lce" "--list-ce" "List GLUE Computing Elements" :flag true :default false]
          ["-lsf" "--list-software" "List GLUE Software" :flag true :default false]
          ["-lse" "--list-se" "List GLUE Storage Elements" :flag true :default false]
          ["-lsa" "--list-sa" "List GLUE Storage Areas" :flag true :default false]
          ["-lvo" "--list-vo" "List GLUE Virtual Organizations" :flag true :default false]
          )]
    ;;
    ;; Display help if -h was passed in the argument list, then exit
    ;;
    (when (:help opts)
      (println (project-description))
      (println banner)
      (System/exit 0))

    ;;
    ;; Check if the user passed dn and password options
    ;;
    (if
      (and (:dn opts) (:password opts))
      (dispatch-cli-queries opts)
      (do
        (println "\nError: Missing password or DN\n")
        (println banner))
      )))