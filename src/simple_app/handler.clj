(ns simple-app.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :as reload]
            [ring.middleware.session :refer [wrap-session]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.util.response :refer [redirect]]
            [simple-app.view :as view]
            [simple-app.db :as db]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [check encrypt]]
            [buddy.auth.backends.session :refer [session-backend]]
            [hiccup.core :refer [html]]
            [clojure.set :refer [union]]
            )
  (:gen-class))

(def unique-index-violation-code 23505)

(defn login
  "hashmap(request) -> hashmap(response)
  Login handler. Check if password match with hash stored in database
  and redirects with logged-in session in response map or with error."
  [request]
  (let [{:keys [username password]} (:params request) ;; get :username and :password keys from request map.
        user (first (db/get-user username))] ;; get user from db. if user exists it will return list with one user.
    (if (check password (:PASSWORD user)) ;; buddy.hashers/check checks password from request against hash from db.
      (-> (redirect "/") (assoc :session {:identity (:USERNAME user)})) ;; if password is correct redirect with new identity
      (-> (redirect "/") (assoc :flash {:error "Invalid username or password."})))) ;; else redirect with invalid passowrd error
  )

(defn register
  "hashmap(request) -> hashmap(response)
  Registration handler. Checks if password equals to confirmation, encrypts password and writes to db.
  Catches unique index violation if user already exists, will throw any other db error."
  [request]
  (let [{:keys [username password password-conf]} (:params request)] ;; get params from request map
    (if (= password password-conf) ;; check if password match with confirmation
      (try (do (db/add-user username (encrypt password)) ;; try to insert new user into database
               (redirect "/")) ;; and redirect to home page
           (catch org.h2.jdbc.JdbcSQLException e ;; catch database exception
             (if (= unique-index-violation-code (.getErrorCode e)) ;; if error code to unique index violation code
               (-> (redirect "/") ;redirect to home page with error added to response map
                   (assoc :flash {:error (str "User with username \"" username "\" already exists.")}))
               (str (.getMessage e) " code: " (.getErrorCode e))))) ;; if other db exception throw it.
      (-> (redirect "/") (assoc :flash {:error "Password does not match with confirmation."})))) ; if passwords doesn't macht
                                                                                                 ; redirect with error
  )

(defn logout
  "hashmap(request) -> hashmap(response)
  Logout handler. Redirects to '/' with empty session."
  [request]
  (-> (redirect "/") (assoc :session {}))
  )

(defn home
  "hashmap(request) -> hashmap(response)
  Home page handler. Check if user is logged in and render login page or routes table."
  [request]
  (let [{:keys [field dir] :or {:field "ID" :dir "DESC"}} (:params request) ;; get sorting from params or use default
        sort {:field field :dir dir}]
    (if-not (authenticated? request) ;; check if user is logged in
      (view/login-page (-> request :flash :error)) ;; if not render login page
      (view/home-page sort (db/get-routes sort))))) ;; else render routes table and pass routes data from db to it

(defn new-route-page
  "hashmap(request) -> hashmap(response)
  New route page handler. Redirect to home if not authenticated."
  [request]
  (if (authenticated? request)
    (view/new-route (:params request))
    (redirect "/"))
  )


(defn new-route
  "hashmap(request) -> hashmap(response)
  New route form handler."
  [request]
  (db/add-route (:params request)) ;; pass request params to database add-route function
  (redirect "/") ;; then redirect to home page
  )

(defn combine-routes
  "hashmap(request) -> hashmap(response)
  Combine checkpoints from two routes."
  [request]
  (let [{:keys [route1 route2]} (:params request)]
    (union (db/get-route-checkpoints route1)
           (db/get-route-checkpoints route2)))
  )

(defn find-routes
  "hashmap(request) -> hashmap(response)
  doc-string"
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

(defn wrap-auth
  "handler -> handler
  Combine authorization and authentication middleware into one function."
  [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(def app
 ;; wrap routes with middleware
  (-> app-routes
      wrap-auth ;; wrap authentication
      wrap-anti-forgery ;; wrap csrf token protection
      (wrap-session {:cookie-attrs {:http-only true}}) ;; wrap session middleware
      (wrap-defaults (-> site-defaults ;; wrap default ring middleware
                         (assoc-in [:security :anti-forgery] false)
                         (dissoc :session)))
      reload/wrap-reload)) ;; wrap reload middleware for development to not restart server on each route change
