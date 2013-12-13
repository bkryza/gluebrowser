(defproject gluebrowser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojars.pntblnk/clj-ldap "0.0.9"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.unboundid/unboundid-ldapsdk "2.3.5" ]]
  :main gluebrowser.core)
