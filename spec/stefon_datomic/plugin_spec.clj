(ns stefon-datomic.plugin-spec

  (:require [speclj.core :refer :all]
            [stefon.shell :as shell]
            [stefon.shell.plugin :as plugin]))


(describe "plugin should be able to attach to a running Stefon instance"

          (it "Should attach to a running Stefon instance"

              (let [sys1 (shell/create-system)
                    sys2 (shell/start-system sys1)

                    handler-fn (fn [message] (println "Zzz ..."))
                    sender-fn (plugin/attach-plugin @sys2 handler-fn)

                    result-promise (sender-fn {:stefon.domain {:parameters nil}})]

                (should-not-be-nil @result-promise)
                (should= {:posts [], :assets [], :tags []} @result-promise)

                (should= 1 1)))

          ;; check that kernel / shell is running

          ;; attach itself to kernel

          ;; check if configured DB exists
          ;;   i. if not, generate schema
          ;;   ii. create DB w/ schema

          ;; make CRUD functions from generated schema
          ;;  post(s)
          ;;  asset(s)
          ;;  tag(s)
          ;;  find-by relationships
          ;;    posts > tags
          ;;    tags > posts
          ;;    assets > post

          )
