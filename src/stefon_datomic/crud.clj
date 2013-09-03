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

        ;; xx (println "Zzz > " (map (fn [inp] (cons '?e inp)) (seq constraints-w-ns)))

        ;; we expect a structure like... ((:posts/title t) (:posts/content-type c/t))
        names-fn #(-> % first name (string/split #"/") first (->> (str "?")) name)
        param-names (map names-fn
                         (seq constraints-w-ns))
        param-values (map last (seq constraints-w-ns))

        xx (println "param-names > " param-names)
        xx (println "param-values > " param-values)

        constraints-final (->> constraints-w-ns
                               seq
                               (map (fn [inp]
                                      ['?e (first inp) (names-fn inp)] ))
                               flatten
                               (into []))

        xx (println "constraints-final > " constraints-final)

        ;; expression-final [:find '?e :where constraints-final]
        ;; expression-final '[:find ?e :in $ ?content-type :where [?e :posts/title "t" ?e :posts/content-type ?content-type]]  ;; Quote this to prevent attemped evaluation of the symbol ?e
        ;; expression-final '[:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title ?e :posts/content-type ?content-type]]  ;; Quote this to prevent attemped evaluation of the symbol ?e


        ;;expression-final `[:find ?e :in $ ~@param-names :where ~constraints-final]


        expression-final `[:find ~@(->> param-names (cons '$) (cons :in) (cons '?e)) :where ~constraints-final]
        ;;expression-final '[:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title ?e :posts/content-type ?content-type]]

        xx (println "expression-final 1 > " expression-final)
        xx (println "expression-final 2 > " (type expression-final))
        xx (println "expression-final 3 > " (type '[:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title ?e :posts/content-type ?content-type]]))
        ]

    (datomic/q expression-final (datomic/db conn) "t" "c/t")

    ;;(def asdf '[?e :posts/title ?title ?e :posts/content-type ?content-type])
    ;;(println "1 > " constraints-final)
    ;;(println "2 > " asdf)
    ;;(datomic/q (quote [:find ?e :in $ ?title ?content-type :where asdf]) (datomic/db conn) "t" "c/t")


    ;;(datomic/q (quote [:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title ?e :posts/content-type ?content-type]]) (datomic/db conn) "t" "c/t")

    ;;(datomic/q '[:find ?e :in $ ?title ?content-type :where [?e :posts/title ?title ?e :posts/content-type ?content-type]] (datomic/db conn) "t" "c/t")

    ;;(datomic/q '[:find ?e :in $ :where [?e :posts/title]] (datomic/db conn))

    ))

(defn retrieve [conn constraint-map]

  (let [id-set (retrieve-entity conn constraint-map)
        entity-set (map (fn [inp]
                          (datomic/entity (datomic/db conn) inp))
                        (first id-set))]

    (println ">> Entity-set 1 > " (datomic/entity (datomic/db conn) (ffirst id-set)))
    (println ">> Entity-set 2 > " (datomic/touch (datomic/entity (datomic/db conn) (ffirst id-set))))


    ))
