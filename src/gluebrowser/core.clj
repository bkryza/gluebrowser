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
  [host port dn pass query-fn & args]
  (try
    (let [url (join ":" [host port])
          ldap-server (ldap/connect {:host     url
                                     :bind-dn  dn
                                     :password pass})]
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
  (cond
    ;
    ; TODO: loop over all command line query arguments
    ;
    ; TODO: Refactor query-server method so that the connection parameters host, port, dn, password
    ; can be passed in a single dictionary
    ;
    (:list-sites opts) (query-server (:host opts)
                                     (:port opts)
                                     (:dn opts)
                                     (:password opts)
                                     gluequeries/glue-object-list-query
                                     :list-sites)
    (:list-se opts) (query-server (:host opts)
                                     (:port opts)
                                     (:dn opts)
                                     (:password opts)
                                     gluequeries/glue-object-list-query
                                     :list-se)
    ;
    ; Handle other list-* options
    ;
    :else TODO))


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
