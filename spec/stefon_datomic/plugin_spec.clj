(ns stefon-datomic.plugin-spec

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


(describe "Plugin should be able to attach to a running Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))

          (it "Should attach to a running Stefon instance"

              (let [sys1 (shell/create-system)
                    sys2 (shell/start-system sys1)

                    handler-fn (fn [message] (println "Handler message, after :stefon.domain > " message))
                    sender-fn (plugin/attach-plugin @sys2 handler-fn)

                    result-promise (sender-fn {:stefon.domain {:parameters nil}})]

                (should-not-be-nil @result-promise)
                (should= {:posts [], :assets [], :tags []} @result-promise)

                (should= 1 1)))


          (it "Should return a list of domain schema"

              (let [sys1 (shell/create-system)
                    sys2 (shell/start-system sys1)

                    handler-fn (fn [message] (println "Handler message, after :stefon.domain.schema > " message))
                    sender-fn (plugin/attach-plugin @sys2 handler-fn)

                    result-promise (sender-fn {:stefon.domain.schema {:parameters nil}})]

                (println ">> " @result-promise)
                (should-not-be-nil @result-promise)
                (should= '(:posts :assets :tags) (keys @result-promise))
                (should= domain-schema @result-promise)))


          (it "Should get the plugin's configuration"

              (let [config (pluginD/get-config)]
                (should-not-be-nil config)
                (should= '(:dev :prod) (keys config))))


          (it "Should throw an exception if DB has not been created, and we connect to DB"

              (should-throw Exception (pluginD/connect-to-db :dev)))


          (it "Should be able to create a DB"

              (let [rvalue (pluginD/create-db :dev)]

                (should-not-be-nil rvalue)
                (should rvalue)))


          (it "Should be able to Generate a DB Schema from a Domain Schema"

              (let [db-schema (pluginD/generate-db-schema domain-schema)]

                (should-not-be-nil db-schema)
                (should= 12 (count db-schema))

                (should= :assets/id (-> db-schema first :db/ident))
                (should= :db.type/uuid (-> db-schema first :db/valueType))
                (should= :db.cardinality/one (-> db-schema first :db/cardinality))))


          (it "Should be able to load schema into a created DB"

              ;; create DB
              (pluginD/create-db :dev)


              (let [
                    ;; connect
                    conn (pluginD/connect-to-db :dev)

                    ;; load schema
                    db-schema (pluginD/generate-db-schema domain-schema)
                    result (pluginD/load-db-schema conn db-schema)]

                (should-not-be-nil result)
                (should (future? result))
                (should (map? @result))))


          (it "Should connect or create a DB - Part 1"

              (let [
                    one (if-not (shell/system-started?)
                          (shell/start-system))
                    rhandler (fn [message] (println "Part 1 handler called"))
                    sfunction (shell/attach-plugin rhandler)

                    domain-schema-promise (sfunction {:stefon.domain.schema {:parameters nil}})

                    ;; try calling when db DOES NOT exist
                    conn (pluginD/connect-or-create @domain-schema-promise :dev)
                    ]

                (should-not-be-nil conn)
                (should= datomic.peer.LocalConnection (type conn))))


          (it "Should connect or create a DB - Part 2"

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


          (it "Should attach itself to the kernel - Main 001"

              ;; check to see if kernel / system is running
              (should-throw Exception (pluginD/plugin :dev)))


          (it "Should attach itself to the kernel - Main 002"

              (let [one (shell/start-system)
                    senderF (pluginD/plugin :dev)]

                ;; check result is the sender-function
                (should-not-be-nil senderF)
                (should (fn? senderF))

                ;; check assignment of sender function
                (should-not-be-nil (:send-fn @pluginD/communication-pair))
                (should (fn? (:send-fn @pluginD/communication-pair)))))


          (it "Should attach itself to the kernel - Main 002"


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


          ;; make CRUD functions from generated schema

          ;;  post(s)
          (it "Should save created post(s) to Datomic"

              ;; create a post, then check the DB
              1)

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

          ;;  asset(s)
          ;;  tag(s)
          ;;  find-by relationships
          ;;    posts > tags
          ;;    tags > posts
          ;;    assets > post

          )
