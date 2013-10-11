(defproject stefon-datomic "0.1.1-SNAPSHOT"
  :description "A Datomic database adapter for the Stefon blogging engine."
  :url "https://github.com/twashing/stefon-datomic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 #_[com.datomic/datomic-free "0.8.4143"
                  :exclusions [org.slf4j/slf4j-nop org.slf4j/log4j-over-slf4j]]
                 [com.datomic/datomic-free "0.8.4143"]
                 #_[org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 [prismatic/schema "0.1.5"]
                 [cljs-uuid "0.0.4"]
                 ]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [
                                  [org.clojure/tools.trace "0.7.6"]
                                  [org.clojure/tools.namespace "0.2.3"]
                                  [speclj "2.5.0"]]}}

  :plugins [[speclj "2.5.0"]]

  :test-paths ["spec"]
  )
