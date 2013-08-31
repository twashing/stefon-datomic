(ns stefon-datomic.crud
  (:require [stefon-datomic.config :as config]))


(defn find-mapping [mkey]

  (let [cfg (config/get-config)]
    (mkey cfg)))


(defn create [conn domain-key datom-map]

  ;; find the mapping

  ;; insert mapped function & preamble

  ;; add namespace to map keys

  ;; transact to Datomic
  )
