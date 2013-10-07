(ns stefon-datomic.plugin-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]

            [stefon.shell :as shell]
            [stefon.domain :as domain]
            [stefon.shell.plugin :as plugin]
            [stefon-datomic.plugin :as pluginD]
            [stefon-datomic.crud :as crud]))


(def config (load-string (slurp (io/resource "stefon-datomic.edn"))))
(def domain-schema {:posts
                    [{:name :id :cardinality :one :type :string}
                     {:name :title :cardinality :one :type :string}
                     {:name :content :cardinality :one :type :string}
                     {:name :content-type :cardinality :one :type :string}
                     {:name :created-date :cardinality :one :type :instant}
                     {:name :modified-date :cardinality :one :type :instant}
                     {:name :assets :cardinality :many :type :ref}
                     {:name :tags :cardinality :many :type :ref}],
                    :assets
                    [{:name :id :cardinality :one :type :string}
                     {:name :name :cardinality :one :type :string}
                     {:name :type :cardinality :one :type :string}
                     {:name :asset :cardinality :one :type :string}],
                    :tags
                    [{:name :id :cardinality :one :type :string}
                     {:name :name :cardinality :one :type :string}]})


(describe "Plugin should be able to attach to a running Stefon instance => "

            (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


            (it "Testing the core plugin function"

              (let [result (atom nil)
                    tee-fn (fn [msg]

                             (println "<< RECIEVEING Message >> " msg)
                             (swap! result (fn [inp] msg)))

                    step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)
                    step-three (pluginD/add-receive-tee tee-fn)]


                ;; create a post, then check the DB
                (shell/create :post "t" "c" "c/t" "0000" "1111" nil nil)

                (should-not-be-nil @result)
                (should (map? @result))
                (should= :plugin.post.create (-> @result keys first))
                (should= {:title "t" :content "c" :content-type "c/t" :created-date "0000" :modified-date "1111" :assets nil :tags nil}
                         (-> @result :plugin.post.create :message :stefon.post.create :parameters)))) )


(describe "Integrate CRUD with plugin messages"


          (it "Testing kernel / plugin connection with CREATE"

              (shell/stop-system)
              (let [
                    result (pluginD/bootstrap-stefon)
                    conn (:conn result)

                    step-two (pluginD/plugin :dev)

                    create-promise (promise)
                    retrieve-promise (promise)

                    step-four (pluginD/subscribe-to-braodcast (fn [msg]

                                                                (println "<< IN broadcast > 1 >>" msg)

                                                                (deliver create-promise (-> msg :plugin.post.create :message))
                                                                (deliver retrieve-promise (crud/retrieve conn :post {:title "my post"})) ))

                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013")) ]

                ;; CREATE Post
                (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])

                (should-not-be-nil @retrieve-promise)
                (should-not (empty? @retrieve-promise))
                (should= 1 (count @retrieve-promise)) ))


          (it "Testing kernel / plugin connection with RETRIEVE"

              (shell/stop-system)
              (let [
                    result (pluginD/bootstrap-stefon)
                    conn (:conn result)

                    step-two (pluginD/plugin :dev)

                    step-four (pluginD/subscribe-to-braodcast (fn [msg]

                                                                (println "<< IN broadcast > 2 >>" msg)


                                                                #_test-retrieved #_(shell/retrieve :post (:id @cpost))
                                                                #_aaa #_(println ">> cpost > " (keys @test-retrieved))
                                                                ))

                    ;; RETRIEVE Post
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    ]

                (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])

                #_(should-not-be-nil @test-retrieved)
                #_(should= stefon.domain.Post (type @test-retrieved))
                #_(should= '(:id :title :content :content-type :created-date :modified-date :assets :tags) (keys @test-retrieved))

                ))

          #_(it "Testing kernel / plugin connection with UPDATE"

              (let [step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)

                    result (pluginD/bootstrap-stefon :dev true)
                    conn (:conn result)

                    ;; CREATE Post
                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))
                    cpost (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])

                    ww (println "... cpost > " @cpost)
                    xx (crud/retrieve conn :post {:posts/title "my post"})
                    yy (println "... This is getting FRUSTRATING > " xx)

                    ;;upost (shell/update :post (:id @cpost) {:title "new title" :content "new content"})

                    ;;test-updated (crud/retrieve conn :post {:posts/id (:id @cpost)})
                    ;;aaa (println ">> upost > " test-updated)

                    ;; UPDATE Post
                    ;;upost (shell/update id-123 {:title "another title"})
                    ]

                (it "one" 1)))

          #_(it "Testing kernel / plugin connection with DELETE"

              (let [step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)

                    result (pluginD/bootstrap-stefon :dev true)
                    conn (:conn result)

                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))

                    ;; CREATE Post
                    cpost (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])
                    test-created (crud/retrieve conn :post {:title "my post"})
                    aaa (println ">> cpost > " test-created)

                    ;; DELETE Post
                    ;;dpost (shell/delete id-123)

                    ]

                (it "one" 1)))

          #_(it "Testing kernel / plugin connection with FIND"

              (let [step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)

                    result (pluginD/bootstrap-stefon :dev true)
                    conn (:conn result)

                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))

                    ;; CREATE Post
                    cpost (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])
                    test-created (crud/retrieve conn :post {:title "my post"})
                    aaa (println ">> cpost > " test-created)

                    ;; FIND Post
                    ;;fpost (shell/find :post {:title "my post"})
                    ;;bbb (println ">> fpost > " @fpost)
                    ]

                (it "one" 1)))

          #_(it "Testing kernel / plugin connection with CREATE with RELATIONSHIPS"

              (let [step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)

                    result (pluginD/bootstrap-stefon :dev true)
                    conn (:conn result)

                    date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))

                    ;; CREATE Post
                    cpost (shell/create :post "my post" "my content" "text/md" date-one date-one [] [])
                    test-created (crud/retrieve conn :post {:title "my post"})
                    aaa (println ">> cpost > " test-created)

                    ;; CREATE Post w/ related Assets and Tags
                    ;; rpost ...

                    ]

                )))
