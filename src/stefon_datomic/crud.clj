(ns stefon-datomic.crud
  (:require [clojure.string :as string]
            [clojure.pprint :as pp]
            [datomic.api :as d]
            [stefon-datomic.config :as config]))


(defn add-entity-ns
  "Prepends namespace ekey to all keys of datom-map. Ekey may be a symbol, keyword or string.
   So it turns a datom-map like A into B, given an entity-key of :post

   A) {:title t :content c :content-type c/t :created-date 0000 :modified-date 1111}
   B) {:post/title t :post/content c :post/content-type c/t :post/created-date 0000 :post/modified-date 1111}"
  [ekey datom-map]
  (reduce-kv (fn [a k v]
               (assoc a (keyword
                         (name ekey)
                         (name k))
                      v))
             {}
             datom-map))

(defn hset-to-cset
  "Put java.util.HashSet into a regular Clojure set"
  [hset]
  (map first (into #{} hset)))

(defn vivify-datomic-entity [the-db eid]
  (d/touch (d/entity the-db eid)))

(defn convert-domain-ns
  "Simply converts a regular domain key, like :post, to the datomic ns representation, :posts"
  [domain-key]

  (-> domain-key name (str "s") keyword))


(defn create [conn domain-key datom-map]

  {:pre [(keyword? domain-key)
         (map? datom-map)]}

  (let [
        one (str "plugin." (name domain-key) ".create")
        lookup-key (keyword one)

        ;; ensure we are adding ID strings to entities
        datom-w-id (assoc datom-map :id (str (java.util.UUID/randomUUID)))

        ;; add namespace to map keys
        entity-w-ns (add-entity-ns (convert-domain-ns domain-key) datom-w-id)
        adatom (assoc entity-w-ns :db/id (datomic.api/tempid :db.part/user)) ]

    ;; transact to Datomic
    @(datomic.api/transact conn [adatom])))


(defn create-relationship
  "This function allows us to group together, the creation of a post, with assets & tags

     can create 1 post with many assets & tags
     can create 1 post with many assets
     can create 1 post with many tags"
  [conn entity-list]

  ;; ensure it's a list
  ;; ensure just 1 or 0 :post
  ;; namespaces should be fully qualified for datomic
  {:pre [(not (nil? entity-list))
         (vector? entity-list)
         (< 2 (count (some #(:posts/title %) entity-list)))]}

  (let [
        ;; 1. put i. tempid(s) and ii. ID(s) in entities
        entities-w-ids (into [] (map #(assoc % :posts/id (str (java.util.UUID/randomUUID)) :db/id (datomic.api/tempid :db.part/user)) entity-list))


        ;; 2. assign tempid(s) of assets to post
        assets (filter (fn [inp]
                         (some #(= :assets/name %)
                               (keys inp)))
                       entities-w-ids)
        asset-ids (into [] (map #(:db/id %) assets))


        ;; 3. assign tempid(s) of tags to post
        tags (filter (fn [inp]
                       (some #(= :tags/name %)
                             (keys inp)))
                     entities-w-ids)
        tag-ids (into [] (map #(:db/id %) tags))


        ;; 4. update the post
        indexed-items (map-indexed (fn [idx ech] [idx ech])
                                   entities-w-ids)
        filtered-post (filter #(:posts/title (second %)) indexed-items)
        post-index (ffirst filtered-post)

        post-w-assets (assoc-in entities-w-ids [post-index :posts/assets] asset-ids)
        post-w-tags (assoc-in post-w-assets [post-index :posts/tags] tag-ids)


        ;; put into a list and transact
        transact-list (into [] post-w-tags) ]

    @(d/transact conn transact-list)))


;; RETRIEVE Functions
(defn retrieve-entity [conn domain-key constraint-map]

  (let [constraints-w-ns (add-entity-ns (convert-domain-ns domain-key) constraint-map)


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


(defn retrieve [conn domain-key constraint-map]

  (let [the-db (d/db conn)

        id-set (hset-to-cset (retrieve-entity conn domain-key constraint-map))
        entity-set (map (fn [inp]
                          (vivify-datomic-entity the-db inp))
                        id-set)]
    entity-set))


(defn retrieve-by-id [conn eid]

  (let [result (d/q '[:find ?eid :in $ ?eid :where [?eid]] (d/db conn) eid)
        result-map (into {} (vivify-datomic-entity (d/db conn) (ffirst result)))

        eid (ffirst result)
        final-map (assoc result-map :db/id eid)]

    final-map))


;; UPDATE
(defn update [conn domain-key datom-map]

  {:pre [(keyword? domain-key)
         (map? datom-map)]}

  (let [one (str "plugin." (name domain-key) ".create")
        lookup-key (keyword one) ]

    (println "UPDATE datom > " datom-map)

    ;; transact to Datomic
    @(datomic.api/transact conn [datom-map])))

(defn update-embellish
  "Embellishes the update map with

     i) db namespace prefix on attributes
     ii) db/id"
  [conn domain-key datom-map]

  {:pre [(keyword? domain-key)
         (map? datom-map)]}



  (let [w-ns (add-entity-ns (convert-domain-ns :post) datom-map)

        yy (println "WTF >> " datom-map)
        xx (retrieve-entity conn domain-key datom-map)


        w-id nil]

    (update conn domain-key w-id)))


(defn add-entity-ns
  "Prepends namespace ekey to all keys of datom-map. Ekey may be a symbol, keyword or string.
   So it turns a datom-map like A into B, given an entity-key of :post

   A) {:title t :content c :content-type c/t :created-date 0000 :modified-date 1111}
   B) {:post/title t :post/content c :post/content-type c/t :post/created-date 0000 :post/modified-date 1111}"
  [ekey datom-map]
  (reduce-kv (fn [a k v]
               (assoc a (keyword
                         (name ekey)
                         (name k))
                      v))
             {}
             datom-map))

(defn convert-domain-ns
  "Simply converts a regular domain key, like :post, to the datomic ns representation, :posts"
  [domain-key]

  (-> domain-key name (str "s") keyword))






;; DELETE
(defn delete [conn entity-id]

  {:pre [(-> conn nil? not)
         (-> entity-id nil? not)]}

  @(datomic.api/transact conn [[:db.fn/retractEntity entity-id]] ))


;; LIST
(defn list [conn domain-key]

  (let [the-db (d/db conn)

        the-lookup (keyword (name domain-key) "id") ;; turns :posts "id".. into :posts/id
        id-set (hset-to-cset (d/q {:find ['?e] :where [['?e the-lookup]]} (d/db conn)))
        entity-set (map (fn [inp]
                          (vivify-datomic-entity the-db inp))
                        id-set)]
    entity-set))
