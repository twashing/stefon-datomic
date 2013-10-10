(ns stefon-datomic.crud-posts-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]

            [stefon.shell :as shell]
            [stefon.shell.plugin :as plugin]
            [stefon-datomic.plugin :as pluginD]
            [stefon-datomic.crud :as crud]
            [stefon-datomic.config :as config]))


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


(defn populate-with-posts []

  ;; create DB & get the connection
  (let [result (pluginD/bootstrap-stefon {:system-started? shell/system-started? :start-system shell/start-system :attach-plugin shell/attach-plugin})
        conn (:conn result)

        ;; add datom
        date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
        one (crud/create conn :post {:title "t" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one})
        two (crud/create conn :post {:title "two" :content "two content" :content-type "c/t" :created-date date-one :modified-date date-one})
        three (crud/create conn :post {:title "three" :content "three content" :content-type "c/t" :created-date date-one :modified-date date-one})]

    conn))

(describe "Plugin should be able to capture and persist CRUD messages from a Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


          ;; match incoming key against known actions
          (it "Should give an empty list if the action is not known"

              (let [result (config/find-mapping :fubar)]
                (should-be-nil result)))

          (it "Should return a proper mapping"

              (let [result (config/find-mapping :plugin.post.create)]

                (should-not-be-nil result)
                (should (map? result))
                (should= stefon-datomic.crud/create (:mapped-action result))))


          ;; ====
          ;; make CRUD functions from generated schema


          ;;  post(s)
          (it "Should save created post(s) to Datomic"

              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon {:system-started? shell/system-started? :start-system shell/start-system :attach-plugin shell/attach-plugin})

                    ;; add datom
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    one (crud/create (:conn result) :post {:title "t" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one :assets [] :tags []})

                    qresult (datomic/q '[:find ?e :where [?e :posts/content-type]] (datomic/db (:conn result))) ]

                (should= java.util.HashSet (type qresult))
                (should-not (empty? qresult))))

          (it "Should retrieve a created entity post from Datomic - 001"

              ;; create 3, then get anyone of them - the second
              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon {:system-started? shell/system-started? :start-system shell/start-system :attach-plugin shell/attach-plugin})

                    ;; add datom
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    one (crud/create (:conn result) :post {:title "t" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one})

                    qresult (crud/retrieve-entity (:conn result) :post {:content-type "c/t" :title "t"}) ]

                (should= java.util.HashSet (type qresult))
                (should-not (empty? qresult))))

          (it "Should retrieve a created post from Datomic - 002"

              ;; create 3, then get anyone of them - the second
              (let [conn (populate-with-posts)

                    qresult (crud/retrieve conn :post {:content-type "c/t" :title "t"})
                    qresult-many (crud/retrieve conn :post {:content-type "c/t"})

                    eid (:db/id (first qresult))
                    uresult (crud/retrieve-by-id conn eid)]

                (println "retrieve-by-id RESULT > " uresult)

                (should (map? uresult))
                (should= '(:db/id :posts/modified-date :posts/created-date :posts/content-type :posts/content :posts/title :posts/id) (keys uresult))))

          (it "Should update a created post from Datomic"

              ;; create 3, then update anyone of them - the third
              (let [
                    conn (populate-with-posts)

                    qresult (crud/retrieve conn :post {:content-type "c/t" :title "three"})
                    qresult-many (crud/retrieve conn :post {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))

                ;; now the UPDATE
                (let [
                      eid (:db/id (first qresult))

                      udt-before (assoc (into {} (first qresult))
                                   :db/id eid  ;; for some reason :db/id gets lost... putting it back
                                   :posts/title "fubar" )
                      udt-after (crud/update conn :post udt-before)

                      result-after (crud/retrieve conn :post {:posts/title "fubar"}) ]

                  (should-not (empty? result-after))
                  (should= "three content" (-> result-after first :posts/content)))))

          (it "Should delete a created post from Datomic"

              ;; create 3, then delete anyone of them - the first
              (let [
                    conn (populate-with-posts)

                    qresult (crud/retrieve conn :post {:content-type "c/t" :title "three"})
                    qresult-many (crud/retrieve conn :post {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))

                ;; now the DELETE
                (let [
                      eid (:db/id (first qresult))

                      dlt-before (assoc (into {} (first qresult))
                                   :db/id eid  ;; for some reason :db/id gets lost... putting it back
                                   :posts/title "fubar" )
                      dlt-after (crud/delete conn eid)

                      result-after (crud/retrieve conn :post {:posts/title "fubar"}) ]

                  (should (empty? result-after)) )))

          (it "Should find by attributes: content-type & created-date"

              ;; create 4, 2 txt, and 2 md files; make one of them have a different created-date
              ;;   then find the md files... from the DB
              ;;   then find the one with a different created-date... from the DB
              (let [;; create DB & get the connection
                    result (pluginD/bootstrap-stefon {:system-started? shell/system-started? :start-system shell/start-system :attach-plugin shell/attach-plugin})

                    ;; add datom
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    one (crud/create (:conn result) :post {:title "t" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one})
                    two (crud/create (:conn result) :post {:title "two" :content "two content" :content-type "c/t" :created-date date-one :modified-date date-one})
                    three (crud/create (:conn result) :post {:title "three" :content "three content" :content-type "c/t" :created-date date-one :modified-date date-one})

                    qresult (crud/retrieve (:conn result) :post {:content-type "c/t" :title "t"})
                    qresult-many (crud/retrieve (:conn result) :post {:content-type "c/t"})]

                (should (seq? qresult))
                (should-not (empty? qresult))
                (should= 1 (count qresult))
                (should= 3 (count qresult-many))))

          (it "Should list created posts"

              ;; create 3, then list them out... from the DB
              (let [conn (populate-with-posts)
                    qresult (crud/llist conn :posts)]

                (println "Listing created posts > " qresult)
                (should-not (empty? qresult))
                (should= 3 (count qresult))))

          ;;  asset(s) - binary data is in Fressian (https://github.com/Datomic/fressian)

          ;;  find-by relationships
          ;;    posts > tags
          ;;    tags > posts

          ;;    post > assets
          ;;    assets > post

          (it "Should be able to reject bad input"

              (let [conn (populate-with-posts)

                    ;; ensure no nils
                    e1 (try (crud/create-relationship conn nil)
                            (catch java.lang.AssertionError e e))

                    ;; ensure it's a list
                    e2 (try (crud/create-relationship conn {})
                            (catch java.lang.AssertionError e e))

                    ;; ensure there's at least 1 post
                    e3 (try (crud/create-relationship conn [(crud/add-entity-ns :assets {:name "iss-orbit" :type "image/png" :asset "binarygoo"})
                                                            (crud/add-entity-ns :tags {:name "datomic"})])
                            (catch java.lang.AssertionError e e))]


                (should-not-be-nil e1)
                (should= java.lang.AssertionError (type e1))

                (should-not-be-nil e2)
                (should= java.lang.AssertionError (type e2))

                (should-not-be-nil e3)
                (should= java.lang.AssertionError (type e3))))

          (it "Should be able to put together related IDs"

              (let [conn (populate-with-posts)
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    r1 (crud/create-relationship conn [(crud/add-entity-ns :posts {:title "tree house" :content "c" :content-type "c/t" :created-date date-one :modified-date date-one})
                                                       (crud/add-entity-ns :assets {:name "iss-orbit" :type "image/png" :asset "binarygoo"})
                                                       (crud/add-entity-ns :tags {:name "datomic"})])]

                (should-not-be-nil r1)
                (should (map? r1))
                (should-not-be-nil (:db-before r1))
                (should-not-be-nil (:db-after r1))

                (let [r2 (crud/retrieve conn :post {:title "tree house"})]


                  (println ">> RELATED post > " r2)

                  (should-not (empty? r2))
                  (should-not-be-nil (:posts/assets (first r2)))
                  (should-not-be-nil (:posts/tags (first r2)))))))
