(ns elephantdb.deploy.node
  (:require
   [elephantdb.deploy.crate
    [daemontools :as daemontools]
    [edb :as edb]
    [edb-configs :as edb-configs]]
   [pallet
    [request-map :as request-map]]
   [pallet.crate
    [automated-admin-user :as automated-admin-user]])
  (:use
   [pallet compute core thread-expr]
   [pallet.blobstore :only [blobstore-from-config]]
   [pallet.resource :only [phase]]
   [pallet.configure :only [pallet-config compute-service-properties]]))


(admin-user "elephantdb"
            :private-key-path "~/.ssh/elephantdb"
            :public-key-path "~/.ssh/elephantdb.pub")

(defn- edb-node-spec [ring]
  (let [{:keys [port]} (edb-configs/read-global-conf! ring)]
    (node-spec
     :image {:image-id "us-east-1/ami-08f40561"
             :hardware-id "m1.large"
             :inbound-ports [22 port]})))

(def edb-server-spec 
  (server-spec
   :phases
   {:bootstrap (phase
                (automated-admin-user/automated-admin-user)
                (daemontools/daemontools))
    :configure (phase
                (edb/setup)
                (edb/deploy))}))

(defn edb-group-spec [ring]
  (group-spec
   (str "edb-" ring)
   :node-spec (edb-node-spec ring)
   :extends [edb-server-spec]))


;; TESTING PURPOSES
#_ (converge {(edb-group-spec "dev5") 1}
          :compute (compute-service-from-config-file "backtype")
          :environment
          {:blobstore (blobstore-from-config (pallet-config) ["backtype"])
           :ring "dev5"
           :edb-s3-keys (compute-service-properties (pallet-config)
                                                    ["backtype"])})




