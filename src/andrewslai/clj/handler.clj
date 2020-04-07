(ns andrewslai.clj.handler
  (:gen-class)
  (:require [andrewslai.clj.auth.mock :as auth-mock]
            [andrewslai.clj.env :as env]
            [andrewslai.clj.persistence.core :as db]
            [andrewslai.clj.persistence.postgres :as postgres]
            [andrewslai.clj.persistence.users :as users]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [cheshire.core :as json]
            [clojure.java.shell :as shell]
            [compojure.api.sweet :refer :all]
            [org.httpkit.server :as httpkit]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :as mem]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            ))

;; https://adambard.com/blog/buddy-password-auth-example/


(timbre/merge-config!
  {:appenders {:spit (appenders/spit-appender {:fname "log.txt"})}})

(defn wrap-logging [handler]
  (fn [request]
    (timbre/with-config (get-in request [:components :logging])
      (timbre/info "Request received for: "
                   (:request-method request)
                   (:uri request))
      (handler request))))

(defn init []
  (println "Hello! Starting service..."))

(defn get-sha []
  (->> "HEAD"
       (shell/sh "git" "rev-parse" "--short")
       :out
       clojure.string/trim))

(def backend (session-backend))

(defn is-authenticated? [{:keys [user] :as req}]
  (timbre/info "Is authenticated?" (not (empty? user)))
  (not (empty? user)))

(defn access-error [request value]
  (timbre/info "Not authorized for endpoint")
  {:status 401
   :headers {}
   :body "Not authorized"})

(defn wrap-components [handler components]
  (fn [request]
    (handler (assoc request :components components))))


(defroutes admin-routes
  (GET "/" request (do (timbre/info "User Authorized for /admin/ route")
                       (ok {:message "Got to the admin-route!"}))))

(def bare-app
  (-> {:swagger
       {:ui "/swagger"
        :spec "/swagger.json"
        :data {:info {:title "andrewslai"
                      :description "My personal website"}
               :tags [{:name "api", :description "some apis"}]}}}
      (api

        (GET "/" []
          (-> (resource-response "index.html" {:root "public"})
              (content-type "text/html")))

        (GET "/ping" []
          (ok {:service-status "ok"
               :sha (get-sha)}))

        (context "/articles" {:keys [components]}
          (GET "/" []
            (ok (db/get-all-articles (:db components))))

          (GET "/:article-name" [article-name :as request]
            (ok (db/get-full-article (get-in request [:components :db]) article-name))))

        (GET "/get-resume-info" {:keys [components]}
          (ok (db/get-resume-info (:db components))))

        (GET "/login" []
          (ok {:message "Login get message"}))

        (POST "/login" {:keys [components body session] :as request}
          (let [credentials (-> request
                                :body
                                slurp
                                (json/parse-string keyword))
                user-id (users/login (:user components) credentials)
                updated-session (assoc session :identity user-id)]
            (if user-id
              (do (timbre/info "Authenticated login!")
                  (assoc (redirect "/") :session updated-session))
              (do (timbre/info "Invalid username/password" credentials)
                  (redirect "/login")))))

        (POST "/logout/" request
          (timbre/info "Is authorized for admin?" (is-authenticated? request)))

        (context "/admin" []
          (restrict admin-routes {:handler is-authenticated?
                                  :on-error access-error})))))

(defn wrap-middleware [handler app-components]
  (-> handler
      wrap-logging
      users/wrap-user
      (wrap-authentication backend)
      (wrap-authorization backend)
      (wrap-session (or (:session-options app-components) {}))
      (wrap-resource "public")
      wrap-cookies
      (wrap-components app-components)
      wrap-content-type
      ))

(def app (wrap-middleware bare-app
                          {:db (postgres/->Database postgres/pg-db)
                           :user (users/->UserDatabase postgres/pg-db)
                           :logging (merge timbre/*config* {:level :debug})
                           :session-options
                           {:store (mem/memory-store (atom {}))}}))

(defn -main [& _]
  (init)
  (httpkit/run-server
    (wrap-middleware bare-app
                     {:db (postgres/->Database postgres/pg-db)
                      :user (users/->UserDatabase postgres/pg-db)
                      :logging (merge timbre/*config* {:level :info})
                      :session-options
                      {:store (mem/memory-store (atom {}))}})
    {:port (@env/env :port)}))

(comment
  (-main)

  (let [resume-info (db/get-resume-info (postgres/->Database postgres/pg-db))]
    (clojure.pprint/pprint (:projects resume-info)))

  (db/get-full-article (postgres/->Database postgres/pg-db) "my-first-article")

  (clojure.pprint/pprint
    (first (db/get-article(postgres/->Database postgres/pg-db) "my-second-article")))

  )
