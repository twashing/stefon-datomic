(ns stefon-datomic.plugin

  (:require [clojure.java.io :as io]
            [datomic.api :as datomic]
            ))


(defn get-config []
  (load-string (slurp (io/resource "stefon-datomic.edn"))))

(defn connect-to-db
  ([env]
     (connect-to-db env (get-config)))
  ([env config]
     (datomic/connect (-> config env :url))))

(defn create-db
  ([env]
     (create-db env (get-config)))
  ([env config]
     (datomic/create-database (-> config env :url))))


(defn plugin

  ([]
     (plugin :prod))

  ([env]
     (let [config (get-config)]
       (plugin env config)))

  ([env config]


    ;; check if configured DB exists
    #_(if-let [conn (connect-to-db)]
      1
      2
      )

    ;; if not, i) generate schema

    ;; if not, ii) create DB with schema


    )
)
