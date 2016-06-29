(ns simple-app.view
  (:require [hiccup.def :refer [defhtml defelem]]
            [hiccup.page :as hp]
            [hiccup.element :as el]
            [hiccup.form :as form]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defmacro defpage
  "string -> string -> vector -> edn(function body) -> function(~vecrot(args) -> string(html))
  Macro defines a function that wrapped with blank page template"
  [name title args & fbody]
  `(defn ~name
     ~args
     (blank-page ~title ~@fbody)))

(def include-bootstrap
  "Scripts and stylesheeets required by bootstrap"
  (list (hp/include-js "https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js")

        [:link {:rel "stylesheet"
                :integrity
                "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
                :crossorigin "anonymous"
                :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"}]
        [:script {:integrity
                  "sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS"
                  :crossorigin "anonymous"
                  :src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"}]))


;; empty page template
;; string -> list[vector(hiccup)] -> string(html)
(defhtml blank-page [title & content]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   include-bootstrap
   [:link {:href "https://fonts.googleapis.com/css?family=Open+Sans:400,700,300"
           :rel "stylesheet"
           :type "text/css"}]
   [:title title]
;   (hp/include-css "/css/main.css")
   ]
  [:body
   [:div.main
    content]])

;; bootstrap formated input element with optional value argument
;; string -> string -> string &-> string -> vector(hiccup element)
(defelem input [type name label & [value]]
  [:div.form-group
   [:label.control-label label]
   [:input.form-control {:type type :id name :name name :value value}]])

;; bootstrap formated input element with multiple inputs which stores values in array
;; and submit button with get method to form location with incremented inputs count
;; string -> string -> string -> int &-> string -> vector(hiccup element)
(defelem input-array [type name label checkpoints-cnt & [values]]
  [:div.form-group
   [:label.control-label label]
   (for [n (range checkpoints-cnt)]
     [:input.form-control {:type type
                           :id (str name n)
                           :name (str name "[]")
                           :value (get values n)}])
   [:input {:type :hidden :name :checkpoints-cnt :value (inc checkpoints-cnt)}]
   [:input {:type :submit :formmethod :get :value "Add more"}]])

(defn sort-link
  "hashmap -> string -> string -> vector(hiccup element)
  Generate table header sorting links depended on current sorting."
  [sort field label]
  (if (= field (:field sort)) ;; if current sorting is on this field
    (if  (= (:dir sort) "DESC") ;; if sorting is descending
      (el/link-to (str "/?field=" field "&dir=ASC") (str label "↑")) ;; ascendig sorting link with arrow up in label.
      (el/link-to (str "/?field=" field "&dir=DESC") (str label "↓"))) ;; descending sorting link with arrow down in label
    (el/link-to (str "/?field=" field "&dir=DESC") label))) ;; if there is no sorting on this field
                                                            ;;return descending sorting link on this field

(defpage home-page "Home" [params data]
  [:div.container-fluid
   [:h1 "Routes" [:span.pull-right (el/link-to "/logout" "Logout")]] ;; page header and logout link
   [:h3
    [:div.col-sm-3 (el/link-to "/new-route" "New route")] ;; links
    [:div.col-sm-3 (el/link-to "/combine-routes" "Combine routes")]
    [:div.col-sm-3 (el/link-to "/find-routes" "Find routes")]
    ]
   [:table.table.table-hover.table-striped.panel-body ;;table
   [:thead
    [:tr
     [:th (sort-link params "NAME" "Name")] ;; table headers with sorting links
     [:th (sort-link params "LENGTH" "Length")]
     [:th (sort-link params "SKILL" "Skill level")]
     [:th (sort-link params "TYPE" "Type")]
     [:th (sort-link params "ELEVATION" "Elevation")]]]
   [:tbody
    (for [row data] ;; table body from db data
      [:tr
       [:td (:NAME row)]
       [:td (:LENGTH row)]
       [:td (:SKILL row)]
       [:td (:TYPE row)]
       [:td (:ELEVATION row)]])]]])

(defpage new-route "New Route" [params]
  (let [checkpoints (if-not (:checkpoints-cnt params) ;; if checkpoints count isn't specified in request params
                      2 ;; defaults to 2 checkpoints
                      (Integer. (:checkpoints-cnt params)))] ;; else specified in params count.
    [:div.container-fluid
     [:div [:h1 "New Route"]
      (form/form-to [:post "/new-route"] ;; new route form
                    (anti-forgery-field)
                    (input :text :name "Name: " (:name params))
                    (input :text :length "Length: " (:length params))
                    (input-array :text "checkpoints" "Checkpoints" checkpoints (:checkpoints params))
                    (input :text :skill "Skill: " (:skill params))
                    (input :text :type "Type: " (:type params))
                    (input :text :elevation "Elevation: " (:elevation params))
                    [:div.form-group (form/submit-button "New route")])]]))

(defpage combine-routes "Combine Routes" []
  [:div.container-fluid
   [:div [:h1 "Combine Routes"]
    (form/form-to [:post "/combine-routes"]
                  (anti-forgery-field)
                  (input :text :name "Name: ")
                  (input :text :length "Length: ")
                  (input :text :skill "Skill: ")
                  (input :text :type "Type: ")
                  (input :text :elevation "Elevation: ")
                  [:div.form-group (form/submit-button "Combine")])]])

(defpage find-routes "Find Routes" []
  [:div.container-fluid
   [:div [:h1 "Find Routes"]
    (form/form-to [:post "/find-routes"]
                  (anti-forgery-field)
                  (input :text :name "Name: ")
                  (input :text :length "Length: ")
                  (input :text :skill "Skill: ")
                  (input :text :type "Type: ")
                  (input :text :elevation "Elevation: ")
                  [:div.form-group (form/submit-button "Find")])]])

(defpage login-page "Login" [error]
  [:div.container-fluid
   [:div [:h1 "Welcome"]]
   (when error [:div.alert.alert-danger [:p error]])
   [:h3 "Login"]
   (form/form-to [:post "/login"]
                 (anti-forgery-field)
                 (input :text :username "Username: ")
                 (input :password :password "Password: ")
                 [:div.form-group (form/submit-button "Login")])
   [:h3 "Register"]
   (form/form-to [:post "/register"]
                 (anti-forgery-field)
                 (input :text :username "Username: ")
                 (input :password :password "Password: ")
                 (input :password :password-conf "Confirm password: ")
                 [:div.form-group (form/submit-button "Register")])])
