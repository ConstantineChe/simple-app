(ns simple-app.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :as reload]
            [ring.middleware.session :refer [wrap-session]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :refer [run-server]]
            [simple-app.view :as view]
            [simple-app.db :as db]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [check encrypt]]
            [buddy.auth.backends.session :refer [session-backend]]
            [hiccup.core :refer [html]]
            )
  (:gen-class))

(def unique-index-violation-code 23505)

(defn login
  "Login handler. Check if password match with hash stored in database
  and redirects with logged-in session in response map or with error."
  [request]
  (let [{:keys [username password]} (:params request) ;; get :username and :password keys from request map.
        user (first (db/get-user username))] ;; get user from db. if user exists it will return list with one user.
    (if (check password (:PASSWORD user)) ;; buddy.hashers/check checks password from request against hash from db.
      (-> (redirect "/") (assoc :session {:identity (:USERNAME user)}))
      (-> (redirect "/") (assoc :flash {:error "Invalid username or password."}))))
  )

(defn register
  "Registration handler"
  [request]
  (let [{:keys [username password password-conf]} (:params request)]
    (if (= password password-conf)
      (try (do (db/add-user username (encrypt password))
               (redirect "/"))
           (catch org.h2.jdbc.JdbcSQLException e
             (if (= unique-index-violation-code (.getErrorCode e))
               (-> (redirect "/") (assoc :flash {:error (str "User with username \"" username "\" already exists.")}))
               (str (.getMessage e) " code: " (.getErrorCode e)))))
      (-> (redirect "/") (assoc :flash {:error "Password does not match with confirmation."}))))
  )

(defn logout
  "Logout handler. Redirects to '/' with empty session."
  [request]
  (-> (redirect "/") (assoc :session {}))
  )

(defn home
  "Home page handler"
  [request]
  (let [{:keys [field dir] :or {:field "ID" :dir "DESC"}} (:params request)
        sort {:field field :dir dir}]
    (if-not (authenticated? request)
      (view/login-page (-> request :flash :error))
      (view/home-page sort (db/get-routes sort)))))

(defn new-route-page
  "New route page handler"
  [request]
  (if (authenticated? request)
    (view/new-route (:params request))
    (redirect "/"))
  )


(defn new-route
  "doc-string"
  [request]
  (db/add-route (:params request))
  (redirect "/")
  )

(defn combine-routes
  "doc-string"
  [request]
  )

(defn find-routes
  "doc-string"
  [request]
  (let []
    (view/home-page (db/get-routes {:filters (:params request)})))
  )



(defroutes app-routes
  ;; All application routes are defined here.
  ;; (METHOD "route-pattern" [request destructuring] handler-fn)
  ;; Compojure request destructuring is a little bit different from default clojure destructuring.
  ;; Handler fn can be function of one argument or can be a value that will be passed in response map.
  (GET "/" [] home)
  (GET "/new-route" [] new-route-page)
  (GET "/combine-routes" [] (fn [r] (if (authenticated? r) (view/combine-routes) (redirect "/"))))
  (GET "/find-routes" [] (fn [r] (if (authenticated? r) (view/find-routes) (redirect "/"))))
  (POST "/new-route" [] new-route)
  (POST "/combine-routes" [] combine-routes)
  (POST "/find-routes" [] find-routes)
  (POST "/login" [] login)
  (POST "/register" [] register)
  (GET "/logout" [] logout)
  (route/not-found "Not Found"))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(def app
 ;; wrap routes with middleware
  (-> app-routes
      wrap-auth
      wrap-anti-forgery
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults (-> site-defaults
                         (assoc-in [:security :anti-forgery] false)
                         (dissoc :session)))
      reload/wrap-reload))



(defn -main [& args]
  (println "Server started")
  (run-server #'app {:port 3001}))
