(defproject stefon-datomic "0.1.0-SNAPSHOT"
  :description "A Datomic database adapter for the Stefon blogging engine."
  :url "https://github.com/twashing/stefon-datomic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 #_[stefon "0.1.0-SNAPSHOT"]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]
                                  [speclj "2.5.0"]]}}

  :plugins [[speclj "2.5.0"]]

  :test-paths ["spec"]
  )
