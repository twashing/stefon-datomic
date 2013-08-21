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


(defn generate-db-schema [domain-schema]

  (->>
   (for [key (keys domain-schema)]
     (for [val (key domain-schema)]

       (let [key-name (name key)
             name-name (name (:name val))
             type-name (name (:type val))
             cardinality-name (name (:cardinality val))
             ]

         {:db/id #db/id [:db.part/db]

          :db/ident (keyword (str key-name "/" name-name))
          :db/valueType (keyword (str "db.type/" type-name))
          :db/cardinality (keyword (str "db.cardinality/" cardinality-name))

          :db.install/_attribute :db.part/db})

       ))
   flatten
   (into []))
  )

(defn load-db-schema [conn db-schema]
  (datomic/transact conn db-schema))


(defn connect-or-create []

  )

(defn plugin

  ([]
     (plugin :prod))

  ([env]
     (let [config (get-config)]
       (plugin env config)))

  ([env config]


     ;; attach plugin to kernel


     ;; check if configured DB exists
     #_(if-let [conn (connect-to-db)]
         1
         2
         )

     ;; if not, i) generate schema

     ;; if not, ii) create DB with schema


     )
  )
