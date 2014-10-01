(ns gluebrowser.core
  (:use [clojure.tools.cli :only [cli]])
  (:use [clojure.string :only [split]])
  (:use [clojure.tools.logging :only [info error]])
  (:require [clj-ldap.client :as ldap])
  (:require [gluebrowser.gluequeries :as gluequeries])
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

(defn query-server
  "
  Connects to the ldap server and executes the query on it passing it any argument list
  "
  [connection-data query-fn & args]
  (try
    (let [ldap-server (ldap/connect {:host     (str (:host connection-data) ":" (:port connection-data))
                                     :bind-dn  (:dn connection-data)
                                     :password (:password connection-data)})]
      ;
      ; Execute the query on the ldap-server
      ;
      (info "Querying LDAP")
      (apply query-fn ldap-server args))
    (catch Exception e
      (println "Couldn't connect to LDAP server: " (.getMessage e)))))


;
; Set of all command line attributes with corresponding GUI output descriptions
;
(def query-descriptions {:list-sites         "List of Sites:"
                         :list-services      "List of Services:"
                         :list-clusters      "List of Clusters:"
                         :list-subclusters   "List of Subclusters:"
                         :list-ce            "List of Computing Elements:"
                         :list-software      "List of Software:"
                         :list-se            "List of Storage Elements:"
                         :list-sa            "List of Storage Areas:"
                         :list-vo            "List of Virtual Organizations:"})


(defn print-single-column-list
  "
  Prints a list of results. Prints one result item in a line
  "
  [items]
  (let [printf-string (str " %" (inc (Math/log10 (count items))) "d. %s%n")]
    (loop [unprocessed-items-list items number 1]
      (when (not-empty unprocessed-items-list)
        (printf printf-string number (first unprocessed-items-list))
        (recur
          (rest unprocessed-items-list)
          (inc number))))))


(defn print-double-column-list
  "
  Prints a list of results. Prints one result item in a line spreading results into columns
  "
  [items]
  (let [first-column-width (inc (Math/log10 (count items)))
        second-column-width (apply max (map (comp count first) items))
        printf-string (str " %" first-column-width "d. %-" second-column-width "s %s%n")]
    (loop [unprocessed-items-list items number 1]
      (when-first [first-item unprocessed-items-list]
        (apply printf printf-string number first-item)
        (recur
          (rest unprocessed-items-list)
          (inc number))))))


(defn split-attributes
  "
  Splits human-defind attributes into query-readable form
  "
  [attributes-string]
  (filter not-empty (split attributes-string #",|;")))


(defn dispatch-cli-queries
  "
  This method executes all queries passed on the command line
  "
  [opts]
  (let [connection-data {:host (:host opts) :port (:port opts) :dn (:dn opts) :password (:password opts)}]
    ;handle list queries
    (doseq [arg (keys query-descriptions)]
      (when (arg opts)
        (let [query-result (query-server connection-data gluequeries/glue-object-list-query arg)]
          (println)
          (println (arg query-descriptions))
          (print-double-column-list
            (if (= "0" (:take-top opts))
              query-result
              (take (Integer. (re-find #"\d+" (:take-top opts))) query-result)))
          (println))))
    ;handle id query
    (when (:get-by-id opts)
      (let [query-result (apply
                           query-server
                           connection-data
                           gluequeries/glue-object-query
                           (:get-by-id opts)
                           (->> opts :attributes split-attributes (map keyword)))]
        (println)
        (println "Selected attributes for objest with ID:" (:get-by-id opts))
        (if query-result
          (print-double-column-list query-result)
          (println "Database doesn't contain the given ID."))
        (println)))
    ;handle foreign key queries
    (when (:find-by-foreign-key opts)
      (let [query-result (apply
                           query-server
                           connection-data
                           gluequeries/glue-object-elements
                           (:find-by-foreign-key opts)
                           (split-attributes (:specific-type opts)))]
        (println)
        (println "Objects related to object with ID:" (:get-by-id opts))
        (if (not-empty query-result)
          (print-single-column-list query-result)
          (println "Database doesn't contain the given ID or no items of selected type(s) are related to this object."))
        (println)))
    ))


(defn -main
  "
  The application's main function
  "
  [& args]
  (let [[opts args banner]
    (cli args
      ["-h" "--help" "Show help" :flag true :default false]
      ["-s" "--host" "GLUE LDAP host" :default "127.0.0.1"]
      ["-p" "--port" "GLUE LDAP port" :default 389]
      ["-d" "--dn" "Authentication user DN" :default "cn=admin,Mds-Vo-name=local,o=grid"] ; Wydaje mi się, że na czas samej pracy nad projektem, możęmy ustawić tutaj default, Michał
      ["-w" "--password" "Authentication password"]
      ;list queries
      ["-ls" "--list-sites" "List GLUE Sites" :flag true :default false]
      ["-lv" "--list-services" "List GLUE Services" :flag true :default false]
      ["-lc" "--list-clusters" "List GLUE Clusters" :flag true :default false]
      ["-lsc" "--list-subclusters" "List GLUE Subclusters" :flag true :default false]
      ["-lce" "--list-ce" "List GLUE Computing Elements" :flag true :default false]
      ["-lsf" "--list-software" "List GLUE Software" :flag true :default false]
      ["-lse" "--list-se" "List GLUE Storage Elements" :flag true :default false]
      ["-lsa" "--list-sa" "List GLUE Storage Areas" :flag true :default false]
      ["-lvo" "--list-vo" "List GLUE Virtual Organizations" :flag true :default false]
      ;additional attribute
      ["-t" "--take-top" "Show only selected number of list query results (0 - take all)" :default "0"]
      ;id query
      ["-g" "--get-by-id" "Get item by id"]
      ["-a" "--attributes" "Specify atributes to be listetd while querying by ID (i. e. \"GlueSiteEmailContact\")" :default ""]
      ;foreign key query
      ["-f" "--find-by-foreign-key" "Find elements related to given dn by foreign key"]
      ["-c" "--specific-type" "Limits results of related object query to specific type" :default ""]
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
    (if (and (:dn opts) (:password opts))
      (dispatch-cli-queries opts)
      (do
        (println "\nError: Missing password or DN\n")
        (println banner)))))