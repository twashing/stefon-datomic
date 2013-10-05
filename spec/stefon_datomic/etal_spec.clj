(ns stefon-datomic.etal-spec

  (:require [speclj.core :refer :all]
            [datomic.api :as datomic]
            [clojure.java.io :as io]
            [clojure.set :as set]

            [stefon.shell :as shell]
            [stefon.shell.plugin :as plugin]
            [stefon-datomic.plugin :as pluginD]
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


(describe "Plugin should be able to attach to a running Stefon instance => "

          (before (datomic/delete-database (-> config :dev :url))
                  (shell/stop-system))


          (it "Should attach to a running Stefon instance"

              (let [
                    sys2 (shell/start-system)

                    result-promise (promise)
                    handler-fn (fn [message]

                                 (println "Handler message, after :stefon.domain > " message)
                                 (deliver result-promise message))
                    plugin-result (plugin/attach-plugin sys2 handler-fn)

                    xx ((:sendfn plugin-result) {:id (:id plugin-result) :message {:stefon.domain {:parameters nil}}}) ]

                (should-not-be-nil @result-promise)
                (should= {:posts [], :assets [], :tags []} (:result @result-promise)) ))


          #_(it "Should return a list of domain schema"

              (let [sys1 (shell/create-system)
                    sys2 (shell/start-system sys1)

                    handler-fn (fn [message] (println "Handler message, after :stefon.domain.schema > " message))
                    sender-fn (plugin/attach-plugin @sys2 handler-fn)

                    result-promise (sender-fn {:stefon.domain.schema {:parameters nil}})]

                (println ">> " @result-promise)
                (should-not-be-nil @result-promise)
                (should= '(:posts :assets :tags) (keys @result-promise))
                (should= domain-schema @result-promise)))


          #_(it "Should get the plugin's configuration"

              (let [config (config/get-config)]
                (should-not-be-nil config)
                (should (some #{:dev :prod} (keys config)))))


          #_(it "Should throw an exception if DB has not been created, and we connect to DB"

              (should-throw Exception (pluginD/connect-to-db :dev)))


          #_(it "Should be able to create a DB"

              (let [rvalue (pluginD/create-db :dev)]

                (should-not-be-nil rvalue)
                (should rvalue)))


          #_(it "Should be able to Generate a DB Schema from a Domain Schema"

              (let [db-schema (pluginD/generate-db-schema domain-schema)]

                (should-not-be-nil db-schema)
                (should= 12 (count db-schema))

                (should= :assets/id (-> db-schema first :db/ident))
                (should= :db.type/uuid (-> db-schema first :db/valueType))
                (should= :db.cardinality/one (-> db-schema first :db/cardinality))))


          #_(it "Should be able to load schema into a created DB"

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


          #_(it "Should connect or create a DB - Part 1"

              (let [
                    one (if-not (shell/system-started?)
                          (shell/start-system))
                    rhandler (fn [message] (println "Part 1 handler called"))
                    sfunction (shell/attach-plugin rhandler)

                    domain-schema-promise (sfunction {:stefon.domain.schema {:parameters nil}})

                    ;; try calling when db DOES NOT exist
                    result (pluginD/connect-or-create @domain-schema-promise :dev)
                    ]

                (should-not-be-nil result)
                (should-not-be-nil (:conn result))
                (should= datomic.peer.LocalConnection (type (:conn result)))))


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
                    result (pluginD/connect-or-create @domain-schema-promise :dev)
                    ]

                (should-not-be-nil result)
                (should-not-be-nil (:conn result))
                (should= datomic.peer.LocalConnection (type (:conn result)))))


          #_(it "Should attach itself to the kernel - Main 001"

              ;; check to see if kernel / system is running
              (should-throw Exception (pluginD/plugin :dev)))


          #_(it "Should attach itself to the kernel - Main 002"

              (let [one (shell/start-system)
                    senderF (pluginD/plugin :dev)]

                ;; check result is the sender-function
                (should-not-be-nil senderF)
                (should (fn? senderF))

                ;; check assignment of sender function
                (should-not-be-nil (:send-fn @pluginD/communication-pair))
                (should (fn? (:send-fn @pluginD/communication-pair)))))


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
                (should= {:fu :bar} @response-msg))))
