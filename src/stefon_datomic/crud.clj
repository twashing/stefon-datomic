(ns stefon-datomic.crud
  (:require [datomic.api :as datomic]
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
        adatom (merge entity-w-ns mapped-preamble)]

    ;; transact to Datomic

    (println "Aaa... " mapped-fn)
    (println "Bbb... " adatom)


    #_(mapped-fn conn [adatom])

    (datomic.api/transact conn [{:db/id #db/id [:db.part/db], :posts/modified-date #inst "2013-01-01T08:00:00.000-00:00", :posts/created-date #inst "2013-01-01T08:00:00.000-00:00", :posts/content-type "ct", :posts/content "c", :posts/title "t"}])

    ))
