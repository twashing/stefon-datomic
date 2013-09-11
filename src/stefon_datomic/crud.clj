(ns stefon-datomic.crud
  (:require [clojure.string :as string]
            [datomic.api :as d]
            [stefon-datomic.config :as config]))


(defn find-mapping [mkey] ;; lg: this should be called sth. like
                          ;; config-action-mapping and probably reside
                          ;; in the config namespace
  
  (let [cfg (config/get-config)]
    (-> cfg :action-mappings mkey)))

(defn add-entity-ns
  "Prepends namespace ekey to all keys of datom-map. Ekey may be a
  symbol, keyword or string."
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
  (map first (into #{} hset)) ;; why map first? this should be noted
                              ;; in the doc-string. Otherwise, this
                              ;; function could be identical to core/set
  )

(defn vivify-datomic-entity [the-db eid]
  (d/touch (d/entity the-db eid))) ;; i'd call it entity-touched since
                                   ;; it almost does the same as
                                   ;; d/entity, except touching, like this:
#_(def entity-touched (comp d/touch d/entity))

(defn create [conn domain-key datom-map]
  
  {:pre [(keyword? domain-key)
         (map? datom-map)]}
  
  (let [
        one (str "plugin." (name domain-key) ".create")
        lookup-key (keyword one)
        
        ;; find the mapping
        mapping (find-mapping lookup-key)
        
        ;; insert mapped function & preamble
        mapped-fn (first mapping)
        mapped-preamble (second mapping)  ;; TODO - can't execute this
                                        ; lg: what does execute mean here?
        
        ;; add namespace to map keys
        entity-w-ns (add-entity-ns :posts datom-map)
        
        adatom (assoc entity-w-ns :db/id (datomic.api/tempid :db.part/user)) ]
    
    ;; transact to Datomic
    @(datomic.api/transact conn [adatom]))) ;; draft of simplified
                                            ;; version below. note
                                            ;; that mapped-fn and
                                            ;; mapped-preamble are not
                                            ;; yet used. care to
                                            ;; comment on the structure of :action-mappings?

#_(defn create
    [conn domain-key datom-map]
    (let [[map-fn map-pre] (-> (str "plugin."
                                    (name domain-key)
                                    ".create")
                               keyword
                               find-mapping) ;; lg: i notice they are both not used
          adatom (-> datom-map
                     (->> (add-entity-ns :posts)) ;; seems like it'd
                                                  ;; make more sense
                                                  ;; if add-entity-ns
                                                  ;; took the
                                                  ;; arguments in reversed order
                     (assoc :db/id
                       (datomic.api/tempid :db.part/user)))]
      @(datomic.api/transact conn [adatom])))

(defn retrieve-entity
  ;; lg: I have pasted this directly from my answer from
  ;; stackoverflow. tested it, works. whats wrong with it?
  [conn constraint-map]
  (let [name-fn (comp symbol
                      (partial str "?")
                      name)
        param-names (map name-fn
                         (keys constraint-map))
        param-vals (vals constraint-map)
        constraint-map (add-entity-ns :posts constraint-map)
        where-clause (map #(vector '?e % %2)
                          (keys constraint-map)
                          param-names)
        in-clause (conj param-names '$)
        final-clause (concat [:find '?e]
                             [:in] in-clause
                             [:where] where-clause)]
    (apply d/q final-clause (d/db conn) param-vals)))

(defn retrieve-entity-2
  ;; lg: i don't know what you meant by being able to pass an array of
  ;; args (since the args are already present in the constraint-map i
  ;; don't see why that would make sense. Here is a version that
  ;; returns an incomplete query function that you can invoke with
  ;; such a map (only last line modified)
  [conn constraint-map]
  (let [name-fn (comp symbol
                      (partial str "?")
                      name)
        param-names (map name-fn
                         (keys constraint-map))
        param-vals (vals constraint-map)
        constraint-map (add-entity-ns :posts constraint-map)
        where-clause (map #(vector '?e % %2)
                          (keys constraint-map)
                          param-names)
        in-clause (conj param-names '$)
        final-clause (concat [:find '?e]
                             [:in] in-clause
                             [:where] where-clause)]
    (partial apply d/q final-clause (d/db conn))))


(defn retrieve [conn constraint-map]
  
  (let [the-db (d/db conn)
        
        id-set (hset-to-cset (retrieve-entity conn constraint-map))
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


(defn update [conn domain-key datom-map]
  
  {:pre [(keyword? domain-key)
         (map? datom-map)]}
  
  (let [
        one (str "plugin." (name domain-key) ".create")
        lookup-key (keyword one)
        
        ;; find the mapping
        mapping (find-mapping lookup-key)
        
        ;; insert mapped function & preamble
        mapped-fn (first mapping) ]


    (println "UPDATE datom > " datom-map)

    ;; transact to Datomic
    @(datomic.api/transact conn [datom-map])))

(defn delete [conn entity-id]

  {:pre [(-> conn nil? not)
         (-> entity-id nil? not)]}

  @(datomic.api/transact conn [[:db.fn/retractEntity entity-id]] ))
