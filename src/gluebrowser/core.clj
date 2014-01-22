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

(def TOP 20)

(def TAKETOP true)

(defn query-server
  "
  Connects to the ldap server and executes the query on it passing it any argument list
  "
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

;
; Set of all command line attributes with corresponding GUI output descriptions
;
(def query-descriptions {    :list-sites         "List of Sites:"
                             :list-services      "List of Services:"
                             :list-clusters      "List of Clusters:"
                             :list-subclusters   "List of Subclusters:"
                             :list-ce            "List of Computing Elements:"
                             :list-software      "List of Software:"
                             :list-se            "List of Storage Elements:"
                             :list-sa            "List of Storage Areas:"
                             :list-vo            "List of Virtual Organizations:"})

(defn print-dispatched-result-list
  "
  Prints a list of results. Prints one result item in a line
  "
  [dispatched-result-list]
  (loop [unprocessed-result-list dispatched-result-list number 1]
    (if (not= (count unprocessed-result-list) 0)
      (do
        (printf "%5d. %s%n" number (first unprocessed-result-list))
        (recur (rest unprocessed-result-list) (+ number 1))))))

(defn print-glue-results
  "
  Prints to standard output all query results taken as a map of lists
  "
  [query-results]
  (loop [unprocessed-result-keys (keys query-results)]
    (if (not= (count unprocessed-result-keys) 0)
      (do
        (println)
        (println ((first unprocessed-result-keys) query-descriptions))
        (print-dispatched-result-list (if TAKETOP
                                        (take TOP ((first unprocessed-result-keys) query-results))
                                        ((first unprocessed-result-keys) query-results)))
        (println)
        (recur (rest unprocessed-result-keys))))))

;
; Set of all command line attributes which handle queries
;
(def query-cli-args {       :list-sites         gluequeries/glue-object-list-query
                            :list-services      gluequeries/glue-object-list-query
                            :list-clusters      gluequeries/glue-object-list-query
                            :list-subclusters   gluequeries/glue-object-list-query
                            :list-ce            gluequeries/glue-object-list-query
                            :list-software      gluequeries/glue-object-list-query
                            :list-se            gluequeries/glue-object-list-query
                            :list-sa            gluequeries/glue-object-list-query
                            :list-vo            gluequeries/glue-object-list-query})



(defn dispatch-cli-queries
  "
  This method executes all queries passed on the command line
  "
  [opts]
  (let [connection-data {:host (:host opts) :port (:port opts) :dn (:dn opts) :password (:password opts)}]
    (loop [available-options (keys query-cli-args) acc {}]
      (if (= (count available-options) 0)
        acc
        (if ((first available-options) opts)
          (recur (rest available-options) (assoc acc (first available-options) (query-server connection-data ((first available-options) query-cli-args) (first available-options))))
          (recur (rest available-options) acc))))))


(defn -main
  "The application's main function"
  ;
  ; TODO: Add new args for objects and IDs
  ;
  [& args]
  (let [[opts args banner]
        (cli args
          ["-h" "--help" "Show help" :flag true :default false]
          ["-s" "--host" "GLUE LDAP host" :default "127.0.0.1"]
          ["-p" "--port" "GLUE LDAP port" :default 389]
          ["-d" "--dn" "Authentication user DN" :default "Mds-Vo-name=local,o=grid"] ; Wydaje mi się, że na czas samej pracy nad projektem, możęmy ustawić tutaj default, Michał
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
      (print-glue-results (dispatch-cli-queries opts))
      (do
        (println "\nError: Missing password or DN\n")
        (println banner))
      )))