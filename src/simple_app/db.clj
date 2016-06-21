(ns simple-app.db
  (:require [korma.db :as kdb]
            [korma.core :as kc :refer [select select* insert defentity table values where order]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

;; H2 database connection
(def db-connection (kdb/h2 {:db "./resources/db/simple-app.db"}))


(defn load-config
  "Configure migrations connection and folder."
  []
  {:datastore  (jdbc/sql-database db-connection)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  "This function performs all unfinished migrations.
   It will be invoked by 'lein migrate'"
  [& args]
    (repl/migrate (load-config)))

(defn rollback
  "This funtion preforms rollback by one migration from current state.
  It will be invoced by 'lein rollback'"
  [& args]
  (repl/rollback (load-config)))

;; Korma database connection
(kdb/defdb db db-connection)

;; Users entity with explicit table name because h2 create all names in upper case.
(defentity users
  (table :USERS))

(defentity routes
  (table :ROUTES))

;(print (kc/exec-raw ["SHOW COLUMNS FROM ROUTES;"] :results))

(defn add-user
  "Performs INSERT query into users table."
  [username password]
  (insert users
          (values {:USERNAME username :PASSWORD password})))

(defn get-user
  "SELECT FROM USERS WHERE USERNAME='username';"
  [username]
  (select users (where {:USERNAME [= username]})))

(defn add-route
  "Add new route.
  INSERT query. Values taken from destructured params map.
  Checkpoints vector transformed into comma-separated list using threading macro."
  [{:keys [name length skill type elevation checkpoints]}]
  (let [checkpoints-str (->> checkpoints (interpose ",") (apply str))]
    (insert routes
            (values {:NAME name
                     :LENGTH length
                     :CHECKPOINTS checkpoints-str
                     :SKILL skill
                     :TYPE type
                     :ELEVATION elevation}))))

(defn get-routes
  "SELECT * FROM ROUTES;"
  [sort]
  (let [{:keys [field dir]} sort
        rts (select routes
                    (order (keyword field) (keyword dir)))]
    (prn rts)
    rts))

(defn find-routes
  "doc-string"
  [filters]
  (select routes
          (where filters))
  )
