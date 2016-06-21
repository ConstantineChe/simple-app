(ns simple-app.view
  (:require [hiccup.def :refer [defhtml defelem]]
            [hiccup.page :as hp]
            [hiccup.element :as el]
            [hiccup.form :as form]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defmacro defpage
  "Macro defines a function that wrapped with blank page template"
  [name title args & fbody]
  `(defn ~name
     ~args
     (blank-page ~title ~@fbody)))

(defn include-bootstrap
  "Scripts and stylesheeets required by bootstrap"
  []
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


(defhtml blank-page [title & content]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (include-bootstrap)
   [:link {:href "https://fonts.googleapis.com/css?family=Open+Sans:400,700,300"
           :rel "stylesheet"
           :type "text/css"}]
   [:title title]
;   (hp/include-css "/css/main.css")
   ]
  [:body
   [:div.main
    content]])

(defelem input [type name label & [value]]
  [:div.form-group
   [:label.control-label label]
   [:input.form-control {:type type :id name :name name :value value}]])

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

(defn sort-link [sort field label]
  (if (= field (:field sort))
    (if  (= (:dir sort) "DESC")
      (el/link-to (str "/?field=" field "&dir=ASC") (str label "↓"))
      (el/link-to (str "/?field=" field "&dir=DESC") (str label "↑")))
    (el/link-to (str "/?field=" field "&dir=DESC") label)))

(defpage home-page "Home" [params data]
  [:div.container-fluid
   [:h1 "Routes" [:span.pull-right (el/link-to "/logout" "Logout")]]
   [:h3
    [:div.col-sm-3 (el/link-to "/new-route" "New route")]
    [:div.col-sm-3 (el/link-to "/combine-routes" "Combine routes")] " "
    [:div.col-sm-3 (el/link-to "/find-routes" "Find routes")]
    ]
   [:table.table.table-hover.table-striped.panel-body
   [:thead
    [:tr
     [:th (sort-link params "NAME" "Name")]
     [:th (sort-link params "LENGTH" "Length")]
     [:th (sort-link params "SKILL" "Skill level")]
     [:th (sort-link params "TYPE" "Type")]
     [:th (sort-link params "ELEVATION" "Elevation")]]]
   [:tbody
    (for [row data]
      [:tr
       [:td (:NAME row)]
       [:td (:LENGTH row)]
       [:td (:SKILL row)]
       [:td (:TYPE row)]
       [:td (:ELEVATION row)]])]]])

(defpage new-route "New Route" [params]
  (let [checkpoints (if-not (:checkpoints-cnt params) 2 (Integer. (:checkpoints-cnt params)))]
    [:div.container-fluid
     [:div [:h1 "New Route"]
      (form/form-to [:post "/new-route"]
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
