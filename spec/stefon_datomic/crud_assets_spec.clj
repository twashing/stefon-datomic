(ns stefon-datomic.crud-assets-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]

            [stefon.shell :as shell]
            [stefon.shell.plugin :as plugin]
            [stefon-datomic.plugin :as pluginD]
            [stefon-datomic.crud :as crud]))


(def config (load-string (slurp (io/resource "stefon-datomic.edn"))))
(def domain-schema {:assets
                    [{:name :id, :cardinality :one, :type :uuid}
                     {:name :name, :cardinality :one, :type :string}
                     {:name :type, :cardinality :one, :type :string}
                     {:name :asset, :cardinality :one, :type :string}],
                    :posts
                    [{:name :id, :cardinality :one, :type :uuid}
                     {:name :title, :cardinality :one, :type :string}
                     {:name :content, :cardinality :one, :type :string}
                     {:name :content-type, :cardinality :one, :type :string}
                     {:name :created-date, :cardinality :one, :type :instant}
                     {:name :modified-date, :cardinality :one, :type :instant}],
                    :tags
                    [{:name :id, :cardinality :one, :type :uuid}
                     {:name :name, :cardinality :one, :type :string}]})


(defn populate-with-assets []

  ;; create DB & get the connection
  (let [
        result (pluginD/bootstrap-stefon)
        conn (:conn result)

        ;; add datom

        one (crud/create conn :asset {:name "iss-orbit" :type "image/png" :asset "binarygoo"})
        two (crud/create conn :asset {:name "ecoli-split" :type "video/mpeg" :asset "binarygoo"})
        three (crud/create conn :asset {:name "rumour-has-it" :type "audio/mpeg" :asset "binarygoo"})]

    conn))

(describe "Plugin should be able to capture and persist CRUD messages from a Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


          ;; ====
          ;; make CRUD functions from generated schema

          ;;  asset(s)
          (it "Should save created asset(s) to Datomic"

              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon)

                    ;; add datom

                    one (crud/create (:conn result) :asset {:name "thing" :type "fubar" :asset "stuff"})

                    qresult (datomic/q '[:find ?e :where [?e :assets/type]] (datomic/db (:conn result)))]

                (should= java.util.HashSet (type qresult))
                (should-not (empty? qresult))))

          (it "Should retrieve a created entity asset from Datomic - 001"

              ;; create 3, then get anyone of them - the second
              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon)

                    ;; add datom
                    one (crud/create (:conn result) :asset {:name "thing" :type "fubar" :asset "stuff"})
                    qresult (crud/retrieve-entity (:conn result) :asset {:name "thing" :type "fubar"}) ]

                (should= java.util.HashSet (type qresult))
                (should-not (empty? qresult))))

          (it "Should retrieve a created asset from Datomic - 002"

              ;; create 3, then get anyone of them - the second
              (let [conn (populate-with-assets)

                    qresult (crud/retrieve conn :asset {:name "iss-orbit"})
                    qresult-many (crud/retrieve conn :asset {:asset "binarygoo"})

                    eid (:db/id (first qresult))
                    uresult (crud/retrieve-by-id conn eid)]

                (should= 1 (count qresult))
                (should= 3 (count qresult-many))

                (should (map? uresult))
                (should= '(:db/id :assets/asset :assets/type :assets/name :assets/id) (keys uresult))))

          #_(it "Should update a created asset from Datomic"

              ;; create 3, then update anyone of them - the third
              (let [
                    conn (populate-with-assets)

                    qresult (crud/retrieve conn {:content-type "c/t" :title "three"})
                    qresult-many (crud/retrieve conn {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))

                ;; now the UPDATE
                (let [
                      eid (:db/id (first qresult))

                      udt-before (assoc (into {} (first qresult))
                                   :db/id eid  ;; for some reason :db/id gets lost... putting it back
                                   :assets/title "fubar" )
                      udt-after (crud/update conn :asset udt-before)

                      result-after (crud/retrieve conn {:assets/title "fubar"}) ]

                  (should-not (empty? result-after))
                  (should= "three content" (-> result-after first :assets/content)))))

          #_(it "Should delete a created asset from Datomic"

              ;; create 3, then delete anyone of them - the first
              (let [
                    conn (populate-with-assets)

                    qresult (crud/retrieve conn {:content-type "c/t" :title "three"})
                    qresult-many (crud/retrieve conn {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))

                ;; now the DELETE
                (let [
                      eid (:db/id (first qresult))

                      dlt-before (assoc (into {} (first qresult))
                                   :db/id eid  ;; for some reason :db/id gets lost... putting it back
                                   :assets/title "fubar" )
                      dlt-after (crud/delete conn eid)

                      result-after (crud/retrieve conn {:assets/title "fubar"}) ]

                  (should (empty? result-after)) )))

          #_(it "Should find by attributes: content-type & created-date"

              ;; create 4, 2 txt, and 2 md files; make one of them have a different created-date
              ;;   then find the md files... from the DB
              ;;   then find the one with a different created-date... from the DB
              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon)

                    ;; add datom
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    one (crud/create (:conn result) :asset {:title "t" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one})
                    two (crud/create (:conn result) :asset {:title "two" :content "two content" :content-type "c/t" :created-date date-one :modified-date date-one})
                    three (crud/create (:conn result) :asset {:title "three" :content "three content" :content-type "c/t" :created-date date-one :modified-date date-one})

                    qresult (crud/retrieve (:conn result) {:content-type "c/t" :title "t"})
                    qresult-many (crud/retrieve (:conn result) {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))))

          #_(it "Should list created assets"

              ;; create 3, then list them out... from the DB
              (let [conn (populate-with-assets)
                    qresult (crud/list conn :assets)]

                (println "Listing created assets > " qresult)
                (should-not (empty? qresult))
                (should= 3 (count qresult))))

          ;;  asset(s) - binary data is in Fressian (https://github.com/Datomic/fressian)
          ;;  tag(s)
          ;;  find-by relationships
          ;;    posts > tags
          ;;    tags > posts
          ;;    assets > post

          )
