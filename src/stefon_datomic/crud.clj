(ns stefon-datomic.crud
  (:require [clojure.string :as string]
            [datomic.api :as datomic]
            [stefon-datomic.config :as config]))


(defn find-mapping [mkey]

  (let [cfg (config/get-config)]
    (-> cfg :action-mappings mkey)))


(defn add-entity-ns [ekey datom-map]

  "Turns a datom-map like A into B, given an entity-key of :post

   A) {:title t :content c :content-type c/t :created-date 0000 :modified-date 1111}
   B) {:post/title t :post/content c :post/content-type c/t :post/created-date 0000 :post/modified-date 1111}"

  {:pre [(keyword? ekey)]}

  (let [one (seq datom-map)
        two (map (fn [inp] [(keyword (str (name ekey) "/" (name (first inp))))
                           (second inp)])
                 one)

        zkeys (map first two)
        zvals (map second two)

        final-entity (zipmap zkeys zvals)]

    final-entity))


(defn create [conn domain-key datom-map]

  {:pre [(keyword? domain-key)]}

  (let [
        one (str "plugin." (name domain-key) ".create")
        lookup-key (keyword one)

        ;; find the mapping
        mapping (find-mapping lookup-key)

        ;; insert mapped function & preamble
        mapped-fn (first mapping)
        mapped-preamble (second mapping)

        entity-w-ns (add-entity-ns :posts datom-map)

        ;; add namespace to map keys
        ;;adatom (merge entity-w-ns mapped-preamble)
        adatom (assoc entity-w-ns :db/id #db/id[:db.part/db])]

    ;; transact to Datomic
    (mapped-fn conn [adatom])))


(defn retrieve-entity [conn constraint-map]

  (let [constraints-w-ns (add-entity-ns :posts constraint-map)


        ;; We expect a structure like... ((:posts/title t) (:posts/content-type c/t))... at the end, we need to double-quote the name
        names-fn #(-> % first name (string/split #"/") first (->> (str "?")) symbol #_(->> (list (symbol 'quote))))
        param-names (map names-fn
                           (seq constraints-w-ns))
        param-values (into [] (map last (seq constraints-w-ns)))


        ;; Should provide constraints that look like: [[?e :posts/title ?title] [?e :posts/content-type ?content-type]]
        constraints-final (->> constraints-w-ns
                               seq
                               (map (fn [inp]
                                      ['?e (first inp) (names-fn inp)] ))
                               (into []))

        ;;
        expression-final {:find ['?e]
                          :in ['$ (into [] param-names)]
                          :where constraints-final}

        ;;
        the-db (datomic.api/db conn)]

    (datomic.api/q expression-final the-db param-values) ))


(defn retrieve [conn constraint-map]

  (let [the-db (datomic/db conn)

        id-set (retrieve-entity conn constraint-map)
        entity-set (map (fn [inp]
                          (datomic/touch (datomic/entity the-db inp)))
                        (first id-set))]

    entity-set))


#_expression-intermediate #_`[:find ~@(->> param-names (cons '$) (cons :in) (cons '?e)) :where ~@constraints-final]


;; Should provide an expression that looks like: [:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title] [?e :posts/content-type ?content-type]]
#_expression-final #_(eval `(quote ~expression-intermediate))
