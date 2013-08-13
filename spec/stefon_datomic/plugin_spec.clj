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

                (println ">> 1" result-promise)
                (should-not-be-nil @result-promise)
                (println ">> 2")

                (should= 1 1))))
