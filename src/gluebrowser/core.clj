(ns gluebrowser.core
  (:use [clojure.tools.cli :only (cli)])
  (:use [clojure.string :only (join split)])
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
  (loop [unprocessed-items-list items number 1]
    (when (not-empty unprocessed-items-list)
      (printf (str " %" (inc (Math/log10 (count items))) "d. %s%n") number (first unprocessed-items-list))
      (recur
        (rest unprocessed-items-list)
        (inc number)))))


(defn print-double-column-list
  "
  Prints a list of results. Prints one result item in a line spreading results into columns
  "
  [items]
  (let [first-column-width (inc (Math/log10 (count items)))
        second-column-width (reduce max 0 (map #(count (first %)) items))]
    (loop [unprocessed-items-list items number 1]
      (when (not-empty unprocessed-items-list)
        (printf
          (str " %" first-column-width "d. %-" second-column-width "s %s%n")
          number
          (first (first unprocessed-items-list))
          (second (first unprocessed-items-list)))
        (recur
          (rest unprocessed-items-list)
          (inc number))))))


;
; Set of all command line attributes which handle list queries
;
(def list-query-cli-args #{  :list-sites
                             :list-services
                             :list-clusters
                             :list-subclusters
                             :list-ce
                             :list-software
                             :list-se
                             :list-sa
                             :list-vo})


(defn split-attributes
  "
  Splits human-defind attributes into query-readable form
  "
  [attributes-string]
  (filter #(not= % (keyword "")) (split attributes-string #",|;")))


(defn dispatch-cli-queries
  "
  This method executes all queries passed on the command line
  "
  [opts]
  (let [connection-data {:host (:host opts) :port (:port opts) :dn (:dn opts) :password (:password opts)}]
    ;handle list queries
    (doall
      (map
        #(if (% opts)
          (let [query-result (query-server connection-data gluequeries/glue-object-list-query %)]
            (println)
            (println (% query-descriptions))
            (print-double-column-list
              (if (= (:take-top opts) "0")
                query-result
                (take (Integer. (re-find #"\d+" (:take-top opts))) query-result)))
            (println)))
        list-query-cli-args))
    ;handle id query
    (if (:get-by-id opts)
      (let
        [query-result
         (apply query-server (cons connection-data (cons gluequeries/glue-object-query (cons (:get-by-id opts) (map #(keyword %) (split-attributes (:attributes opts)))))))]
        (println)
        (println (str "Selected attributes for objest with ID: " (:get-by-id opts)))
        (if (nil? query-result)
          (println "Database doesn't contain the given ID.")
          (print-double-column-list query-result))
        (println)))
    ;handle foreign key queries
    (if (:find-by-foreign-key opts)
      (let [query-result (apply query-server (cons connection-data (cons gluequeries/glue-object-elements (cons (:find-by-foreign-key opts) (split-attributes (:specific-type opts))))))]
        (println)
        (println (str "Objects related to object with ID: " (:get-by-id opts)))
        (if (nil? query-result)
          (println "Database doesn't contain the given ID or no items are related to this object.")
          (print-single-column-list query-result))
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
      ["-f" "--find-by-foreign-key" "Find elements related to diven dn by foreign key"]
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
    (if
      (and (:dn opts) (:password opts))
      (dispatch-cli-queries opts)
      (do
        (println "\nError: Missing password or DN\n")
        (println banner))
      )))