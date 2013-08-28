(ns stefon-datomic.crud-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]

            [stefon.shell :as shell]
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


(describe "Plugin should be able to capture and persist CRUD messages from a Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


          #_(it "Should connect or create a DB - Part 2"

              (let [
                    one (if-not (shell/system-started?)
                          (shell/start-system))
                    rhandler (fn [message] (println "Part 2 handler called"))
                    sfunction (shell/attach-plugin rhandler)

                    domain-schema-promise (sfunction {:stefon.domain.schema {:parameters nil}})
                    aa (pluginD/create-db :dev)
                    bb (pluginD/init-db @domain-schema-promise :dev)


                    ;; try calling when db DOES exist
                    conn (pluginD/connect-or-create @domain-schema-promise :dev)
                    ]

                (should-not-be-nil conn)
                (should= datomic.peer.LocalConnection (type conn))))


          #_(it "Should attach itself to the kernel - Main 002"


              ;; check plugin attach
              (let [one (shell/start-system)
                    two (pluginD/plugin :dev)

                    response-msg (atom nil)
                    response-handler (pluginD/add-receive-tee (fn [msg]
                                                                (swap! response-msg (fn [inp] msg))))

                    three (pluginD/send-message {:fu :bar})]

                (should-not-be-nil @response-msg)
                (should (map? @response-msg))
                (should= {:fu :bar} @response-msg)))


          (defn bootstrap-stefon
            "Start the system and create a DB"
            ([]
               (bootstrap-stefon (fn [message])))
            ([handler-fn]
               (let [step-one (if-not (shell/system-started?)
                                (shell/start-system))
                     send-function (shell/attach-plugin handler-fn)

                     domain-schema-promise (send-function {:stefon.domain.schema {:parameters nil}})
                     step-four (pluginD/create-db :dev)
                     step-five (pluginD/init-db @domain-schema-promise :dev)

                     ;; try calling when db DOES exist
                     conn (pluginD/connect-or-create @domain-schema-promise :dev)

                     ])))


          ;; make CRUD functions from generated schema

          ;;  post(s)
          (it "Should save created post(s) to Datomic"

              (let [result (atom nil)
                    tee-fn (fn [msg]
                             (println "<< RECIEVEING Message >> " msg)
                             (swap! result (fn [inp] msg)))
                    step-two (bootstrap-stefon tee-fn)]

                ;; create a post, then check the DB
                (shell/create :post "t" "c" "c/t" "0000" "1111")

                (should-not-be-nil @result)))

          (it "Should retrieve a created post from Datomic"

              ;; create 3, then get anyone of them - the second
              2)

          (it "Should update a created post from Datomic"

              ;; create 3, then update anyone of them - the third
              3)

          (it "Should delete a created post from Datomic"

              ;; create 3, then delete anyone of them - the first
              5)

          (it "Should find by attributes: content-type & created-date"

              ;; create 4, 2 txt, and 2 md files; make one of them have a different created-date
              ;;   then find the md files... from the DB
              ;;   then find the one with a different created-date... from the DB
              6)

          (it "Should list created posts"

              ;; create 3, then list them out... from the DB
              7)

          ;;  asset(s) - binary data is in Fressian (https://github.com/Datomic/fressian)
          ;;  tag(s)
          ;;  find-by relationships
          ;;    posts > tags
          ;;    tags > posts
          ;;    assets > post

          )
