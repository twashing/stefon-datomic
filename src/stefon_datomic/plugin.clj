(ns stefon-datomic.plugin

  (:require [clojure.java.io :as io]
            [datomic.api :as datomic]

            [stefon.shell :as shell]
            [stefon.shell.kernel :as kernel]))


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

(defn generate-db-schema
  "We expect our domain schema to be a map, and have the following form:

   {:assets
    [{:name :id, :cardinality :one, :type :uuid}
     {:name :name, :cardinality :one, :type :string}
    :posts
    [{:name :id, :cardinality :one, :type :uuid}
     {:name :title, :cardinality :one, :type :string}
     {:name :content, :cardinality :one, :type :string}
    :tags
    [{:name :id, :cardinality :one, :type :uuid}
     {:name :name, :cardinality :one, :type :string}]}"
  [domain-schema]

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

          :db.install/_attribute :db.part/db})))
   flatten
   (into [])))

(defn load-db-schema [conn db-schema]
  (datomic/transact conn db-schema))

(defn init-db

  ([domain-schema]
     (init-db domain-schema :prod))

  ([domain-schema env]

     (let [
           ;; i) create DB
           sone (create-db env)
           conn (connect-to-db env)

           ;; ii) generate schema
           db-schema (generate-db-schema domain-schema)]

       ;; iii) load schema into DB
       (load-db-schema conn db-schema))))


(defn connect-or-create

  ([domain-schema]
     (connect-or-create domain-schema :prod))

  ([domain-schema env]

     (try

       ;; check if configured DB exists
       (connect-to-db env)

       ;; otherwise, initialize it
       (catch Exception e
         (if (init-db domain-schema env)
           (connect-to-db env))))))



(def tee-fns (atom []))
(defn receive-fn [message]

  ;; notify tee-fns
  (reduce (fn [rslt echF]
            (echF message)
            rslt)
          []
          @tee-fns)

  ;; based on message, perform action

  )

(def communication-pair (atom {:receive-fn receive-fn
                               :send-fn nil}))


(defn send-message [message]
  ((:send-fn @communication-pair) message))


;; Useful for testing purposes - get alerted when plugin receives a message
(defn add-receive-tee [receiveF]
  (swap! tee-fns (fn [inp]
                   (conj inp receiveF))))


(defn plugin
  "This clears out all tee functions before attaching to the kernel"

  ([] (plugin :prod))

  ([env]
     (let [config (get-config)]
       (plugin env config)))

  ([env config]


     ;; clear tee-fns
     (swap! tee-fns (fn [inp] []))


     ;; attach plugin to kernel
     (if (shell/system-started?)
       (let [send-fn (shell/attach-plugin (:receive-fn @communication-pair))]
         (swap! communication-pair (fn [inp]
                                     (assoc inp :send-fn send-fn)))
         send-fn)
       (throw Exception "stefon-datomic: stefon system not started"))))
