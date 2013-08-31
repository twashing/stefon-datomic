(ns stefon-datomic.crud
  (:require [stefon-datomic.config :as config]))


(defn find-mapping [mkey]

  (let [cfg (config/get-config)]
    (mkey cfg)))
