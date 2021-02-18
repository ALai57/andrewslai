(ns andrewslai.cljs.keycloak
  (:require [keycloak-js :as keycloak-js]
            [re-frame.core :refer [dispatch]]))

(goog-define AUTH_URL "http://172.17.0.1:8080/auth")
(goog-define REALM "test")
(goog-define CLIENTID "test-login")

(def HOST_URL
  (str js/window.location.protocol "//" js/window.location.host))

(def keycloak
  (js/Keycloak (clj->js {:url AUTH_URL
                         :realm REALM
                         :clientId CLIENTID})))

(defn initialize!
  ([keycloak-instance]
   (initialize! keycloak-instance
                (fn [auth?] (js/console.log "Authenticated? " auth?))
                (fn [auth?] (js/console.log "Unable to initialize Keycloak"))))
  ([keycloak-instance success fail]
   (-> keycloak-instance
       (.init (clj->js {:checkLoginIframe false
                        :pkceMethod "S256"}))
       (.then success)
       (.catch fail))))

(defn login! [keycloak]
  (.login keycloak (clj->js {:scope "roles"
                             :prompt "Please login to continue"
                             :redirectUri HOST_URL})))
