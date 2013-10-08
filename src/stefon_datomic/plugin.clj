(ns stefon-datomic.plugin

  (:require [datomic.api :as datomic]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [clojure.core.async :as async :refer :all]

            [stefon.shell :as shell]
            [stefon.shell.kernel :as kernel]

            [stefon-datomic.crud :as crud]
            [stefon-datomic.config :as config]))


(declare communication-pair)


;; DATABASE Functions
(defn connect-to-db
  ([env]
     (connect-to-db env (config/get-config)))
  ([env config]
     (datomic/connect (-> config env :url))))

(defn create-db
  "Returns true if the database was created, false if it already exists."
  ([env]
     (create-db env (config/get-config)))
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
             cardinality-name (name (:cardinality val))]

         {;;:db/id #db/id[:db.part/db]
          :db/id (datomic/tempid :db.part/db)

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
           db-schema (generate-db-schema domain-schema) ]

       ;; iii) load schema into DB
       (load-db-schema conn db-schema))))


(defn connect-or-create

  ([domain-schema]
     (connect-or-create domain-schema :prod))

  ([domain-schema env]

     (try

       ;; check if configured DB exists
       {:init-result nil
        :conn (connect-to-db env)}

       ;; otherwise, initialize it
       (catch Exception e

         (let [init-result (init-db domain-schema env)
               conn (connect-to-db env)]
           {:init-result init-result
            :conn conn})))))



;; CHANNEL Functions
(def tee-fns (atom []))
(defn receive-fn [message]

  ;; notify tee-fns
  (reduce (fn [rslt echF]
            (echF message)
            rslt)
          []
          @tee-fns)

  ;; based on message, perform action
  ;;(println ">> stefon-datomic.plugin > JACKING IN >> " (seq message))


  ;; key / param(s)
  (let [key-params (-> message seq first)

        ;;xx (println "... 1 > " key-params)
        key (first key-params)

        ;;xx (println "... 2 > " key)
        original-key (keyword (string/replace (name key) #"plugin" "stefon"))
        params (-> key-params second :message original-key :parameters)
        origin-id (-> key-params second :id)

        ;;xx (println "... 3 > " params)
        mapped-action (config/find-mapping key)
        mapped-fn (:mapped-action mapped-action)
        mapped-domain-key (:domain-key mapped-action)]


    ;;(println "")
    ;;(println ">> key > " key)
    ;;(println ">> params > " params)
    ;;(println ">> mapped-action > " mapped-action)
    ;;(println ">> mapped-fn > " (type mapped-fn))
    ;;(println ">> domain-key > " mapped-domain-key)
    ;;(println ">> conn > " (:conn @communication-pair))

    (if mapped-fn

      (let [fn1 (partial mapped-fn (:conn @communication-pair))
            fn2 (if mapped-domain-key (partial fn1 mapped-domain-key) fn1)  ;; conditionally apply the domain-key argument
            fn3 (partial fn2 params)

            action-result (try (fn3) (catch Exception e (println ">> Exception > " e)))]

        ;;(println ">> stefon-datomic.plugin > RESULT >> [" {:id (:id @communication-pair) :origin origin-id :result action-result} "]" )

        ((:send-fn @communication-pair) {:id (:id @communication-pair) :origin origin-id :result action-result})
        action-result))))

(def communication-pair (atom {:receive-fn receive-fn
                               :send-fn nil

                               :conn nil}))


(defn send-message [message]
  ((:send-fn @communication-pair) message))


;; Useful for testing purposes - get alerted when plugin receives a message
(defn add-receive-tee [receiveF]
  (swap! tee-fns (fn [inp]
                   (conj inp receiveF))))



(def broadcast-list (atom []))
(defn subscribe-to-braodcast [handlefn]

  (swap! broadcast-list (fn [inp]
                          (conj inp handlefn))))

(defn generic-handler [env message]

  ;;(println ">> generic-handler CALLED > " message)
  (reduce (fn [rslt echf]
            (echf message))
          []
          @broadcast-list))

(defn init-core [env plugin-result]

  (let [conn (promise)
        xx (subscribe-to-braodcast (fn [message]

                                     ;;(println "... subscribe-to-broadcast > " message)

                                     ;; initialize DB
                                     (init-db (:result message) env)

                                     ;; get connection
                                     (deliver conn (connect-to-db env))))]

    ;; get schema
    ((:sendfn plugin-result) {:id (:id plugin-result) :message {:stefon.domain.schema {:parameters nil}}})

    ;; return the connection
    @conn))


;; BOOTSTRAP the System
(defn bootstrap-stefon
  "Start the system and create a DB"

  ([] (bootstrap-stefon :dev true (partial generic-handler :dev)))

  ([env]
     (bootstrap-stefon env true (partial generic-handler env)))

  ([env initialize]
     (bootstrap-stefon env initialize (partial generic-handler env)))

  ([env initialize handlerfn]


     ;; START System
     (if-not (shell/system-started?)
       (shell/start-system))


     ;; ATTACH Plugin
     (let [result (shell/attach-plugin handlerfn)

           cid (:id result)
           sendfn (:sendfn result)
           recievefn (:recievefn result)]

       ;; CONNECT
       (if initialize

         ;; get schema; initialize DB; get connection
         (assoc result :conn (init-core env result))

         ))))


;; PLUGING Function
(defn plugin
  "This clears out all tee functions before attaching to the kernel"

  ([]
     (plugin :prod))

  ([env]
     (let [config (config/get-config)]
       (plugin env config)))

  ([env config]

     ;; clear tee-fns
     (swap! tee-fns (fn [inp] []))
     (swap! broadcast-list (fn [inp] []))


     ;; attach plugin to kernel
     (if (shell/system-started?)
       (let [plugin-result (shell/attach-plugin (:receive-fn @communication-pair))
             bootstrap-result (bootstrap-stefon env)]

         (swap! communication-pair (fn [inp] (assoc inp :conn (:conn bootstrap-result))))
         (swap! communication-pair (fn [inp] (assoc inp :send-fn (:sendfn plugin-result))))
         (swap! communication-pair (fn [inp] (assoc inp :id (:id plugin-result))))
         plugin-result)
       (throw Exception "stefon-datomic: stefon system not started"))))
