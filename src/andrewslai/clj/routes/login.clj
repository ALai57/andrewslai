(ns andrewslai.clj.routes.login
  (:require [andrewslai.clj.api.users :as users-api]
            [andrewslai.clj.entities.user :as user]
            [clojure.spec.alpha :as s]
            [compojure.api.sweet :refer [context defroutes POST]]
            [ring.util.http-response :refer [ok unauthorized]]
            [taoensso.timbre :as log]))

(s/def ::username string?)
(s/def ::password string?)
(s/def ::credentials (s/keys :req-un [::username
                                      ::password]))

(defn handle-authenticated-login
  [{:keys [username id] :as user} session]
  (log/info "Authenticated login!")
  (-> (assoc user :avatar_url (format "users/%s/avatar" username))
      ok
      (assoc :session (assoc session :identity id)
             :cookies {:access-token {:value "SOME-VALUE"}})))

(defroutes login-routes
  (context "/sessions" []
    :tags ["sessions"]
    :coercion :spec
    :components [database]

    (POST "/login" {:keys [body session body-params] :as request}
      :swagger {:summary "Login"
                :consumes #{"application/json"}
                :produces #{"application/json"}
                :parameters {:body ::credentials}
                #_#_:responses {200 {:description "The user that just authenticated"}}}
      (if-let [user (users-api/login database body-params)]
        (handle-authenticated-login user session)
        (do (log/info "Invalid username/password")
            (unauthorized {:message "Unable to login"}))))

    (POST "/logout" []
      (-> (ok {:message "Logout successful"})
          (assoc :session nil)))))
