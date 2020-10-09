(ns andrewslai.clj.persistence.postgres2
  (:require [andrewslai.clj.persistence :as p :refer [Persistence]]
            [andrewslai.clj.utils :refer [validate]]
            [cheshire.core :as json]
            [clojure.java.jdbc :as sql]
            [honeysql.core :as hsql]
            [honeysql.helpers :as hh]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import org.postgresql.util.PGobject))

(extend-protocol sql/IResultSetReadColumn
  org.postgresql.jdbc.PgArray
  (result-set-read-column [pgobj metadata i]
    (vec (.getArray pgobj)))

  PGobject
  (result-set-read-column [pgobj conn metadata]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/parse-string value keyword)
        :else value))))

(defrecord Database [conn]
  Persistence
  (select [this m]
    (sql/query conn (hsql/format m)))
  (transact! [this m]
    (sql/execute! conn (hsql/format m) {:return-keys true})))

(defn select [database m]
  (p/select database m))

;; TODO: Deal with m being a collection or not
(defn insert! [database table m & {:keys [ex-subtype
                                          input-validation]}]
  (when input-validation
    (validate input-validation m :IllegalArgumentException))
  (try+
   (p/transact! database (-> (hh/insert-into table)
                             (hh/values [m])))
   (catch org.postgresql.util.PSQLException e
     (throw+ (merge {:type :PersistenceException
                     :message {:data (select-keys m [:username :email])
                               :reason (.getMessage e)}}
                    (when ex-subtype
                      {:subtype ex-subtype}))))))

(defn update! [database table m username & {:keys [ex-subtype
                                                   input-validation]}]
  (when input-validation
    (validate input-validation m :IllegalArgumentException))
  (try+
   (p/transact! database (-> (hh/update table)
                             (hh/sset m)
                             (hh/where [:= :username username])))
   (catch org.postgresql.util.PSQLException e
     (throw+ (merge {:type :PersistenceException
                     :message {:data (select-keys m [:username :email])
                               :reason (.getMessage e)}}
                    (when ex-subtype
                      {:subtype ex-subtype}))))))

(comment
  (require '[andrewslai.clj.env :as env])
  (require '[honeysql.helpers :as hh])

  (defn pg-conn []
    (-> @env/env
        (select-keys [:db-port :db-host
                      :db-name :db-user
                      :db-password])
        (clojure.set/rename-keys {:db-name     :dbname
                                  :db-host     :host
                                  :db-user     :user
                                  :db-password :password})
        (assoc :dbtype "postgresql")))

  (def example-user
    {:id         #uuid "f5778c59-e57d-46f0-b5e5-516e5d36481c"
     :first_name "Andrew"
     :last_name  "Lai"
     :username   "alai"
     :avatar     nil
     :email      "andrew@andrew.com"
     :role_id    2})

  (p/select (->Database (pg-conn))
            {:select [:*] :from [:users]})

  (p/transact! (->Database (pg-conn))
               (-> (hh/insert-into :users)
                   (hh/values [example-user])))

  (p/transact! (->Database (pg-conn))
               (-> (hh/delete-from :users)
                   (hh/where [:= :users/username (:username example-user)])))

  (p/transact! (->Database (pg-conn))
               (-> (hh/update :users)
                   (hh/sset {:first_name "FIRSTNAME"})
                   (hh/where [:= :username (:username example-user)])))
  )
