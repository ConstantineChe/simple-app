(defproject simple-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :main simple-app.handler
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-devel "1.5.0"]
                 [http-kit "2.1.18"]
                 [buddy "1.0.0"]
                 [hiccup "1.0.5"]
                 [korma "0.4.2"]
                 [ragtime "0.5.2"]
                 [com.h2database/h2 "1.4.192"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler simple-app.handler/app}
  ;; aliases for migration to use "lein migrate" and lein rollback"
  :aliases {"migrate"  ["run" "-m" "simple-app.db/migrate"]
            "rollback" ["run" "-m" "simple-app.db/rollback"]}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
