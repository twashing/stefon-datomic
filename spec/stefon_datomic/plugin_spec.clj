(ns stefon-datomic.plugin-spec

  (:require [speclj.core :refer :all]
            [stefon.shell :as shell]))


(describe "plugin should be able to attach to a running Stefon instance"


          (before (shell/start-system))
          (after (shell/stop-system))

          (it "Should attach to a running Stefon instance"

              (let [handler-fn (fn [message])
                    sender-fn (shell/attach-plugin handler-fn)

                    result-promise (sender-fn {:stefon.domain {:parameters nil}})]

                (should-not-be-nil @result-promise)

                (should= 1 1))))
