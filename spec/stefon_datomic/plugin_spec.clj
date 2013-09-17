(ns stefon-datomic.plugin-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]

            [stefon.shell :as shell]
            [stefon.domain :as domain]
            [stefon.shell.plugin :as plugin]
            [stefon-datomic.plugin :as pluginD]))


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


(describe "Plugin should be able to attach to a running Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


          (it "Testing the core plugin function"

              (let [result (atom nil)
                    tee-fn (fn [msg]
                             ;;(println "<< RECIEVEING Message >> " msg)
                             (swap! result (fn [inp] msg)))

                    step-one (shell/start-system)
                    step-two (pluginD/plugin :dev)
                    step-three (pluginD/add-receive-tee tee-fn)]


                ;; create a post, then check the DB
                (shell/create :post "t" "c" "c/t" "0000" "1111" nil nil)

                (should-not-be-nil @result)
                (should (map? @result))
                (should= :plugin.post.create (-> @result keys first))
                (should= {:title "t" :content "c" :content-type "c/t" :created-date "0000" :modified-date "1111" :assets-ref nil :tags-ref nil} (-> @result :plugin.post.create :parameters)))))

(describe "Integrate CRUD with plugin messages"

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))

          (let [result (pluginD/bootstrap-stefon)
                conn (:conn result)

                date-one (-> (java.text.SimpleDateFormat. "MM/DD/yyyy") (.parse "09/01/2013"))

                ;; CREATE Post
                cpost (shell/create :post "my post" "my content" "text/md" date-one date-one nil nil)
                ;;aaa (println ">> cpost > " @cpost)

                ;; RETRIEVE Post
                rpost (shell/retrieve :post (:id @cpost))
                ;;ccc (println ">> rpost > " @rpost)


                ;; FIND Post
                fpost (shell/find :post {:title "my post"})
                ;;bbb (println ">> fpost > " @fpost)

                ;; UPDATE Post
                ;;upost (shell/update id-123 {:title "another title"})

                ;; DELETE Post
                ;;dpost (shell/delete id-123)

                ;; CREATE Post w/ related Assets and Tags
                ;; rpost ...

                ]

            (it "one" 1)))
