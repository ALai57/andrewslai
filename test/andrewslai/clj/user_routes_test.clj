(ns andrewslai.clj.user-routes-test
  (:require [andrewslai.clj.auth.crypto :as encryption]
            [andrewslai.clj.handler :as h]
            [andrewslai.clj.persistence.postgres-test :as ptest]
            [andrewslai.clj.persistence.users :as users]
            [andrewslai.clj.test-utils :refer [defdbtest]]
            [andrewslai.clj.utils :refer [parse-response-body
                                          body->map
                                          file->bytes]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing]]
            [compojure.api.sweet :refer [api GET POST]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :as mem]
            [ring.mock.request :as mock]
            [andrewslai.clj.persistence.postgres :as postgres]))



;; Modify all DB tests to use this macro!
;;  because tests are failing due to DB conflicts
(comment
  
  (clojure.pprint/pprint (macroexpand-1 '(defdbtest my-test ptest/db-spec
                                           (jdbc/insert! ptest/db-spec
                                                         "users"
                                                         {:username "new-user"
                                                          :first_name "new"
                                                          :last_name "user"
                                                          :id #uuid "160bf449-535e-4902-9138-6c7c104cca0d"
                                                          :email "newuser@andrewslai.com"
                                                          :role_id 2})
                                           (println "Inserted!")
                                           (println "Results!"
                                                    (jdbc/query ptest/db-spec "select * from users")))))

  (defdbtest my-test ptest/db-spec
    (jdbc/insert! ptest/db-spec
                  "users"
                  {:username "new-user"
                   :first_name "new"
                   :last_name "user"
                   :id #uuid "160bf449-535e-4902-9138-6c7c104cca0d"
                   :email "newuser@andrewslai.com"
                   :role_id 2})
    (println "Inserted!")
    (println "Results!"
             (jdbc/query ptest/db-spec "select * from users")))

  (my-test)
  )

(comment 
  (jdbc/with-db-connection [db ptest/db-spec]
    (jdbc/query db "select * from users"))

  (def ^:dynamic the-db ptest/db-spec)

  (jdbc/with-db-transaction [the-db the-db]
    (jdbc/db-set-rollback-only! the-db)
    (println the-db)
    (binding [the-db the-db] ;; rebind dynamic var db, used in tests
      (jdbc/insert! the-db "users" {:username "new-user"
                                    :first_name "new"
                                    :last_name "user"
                                    :id #uuid "160bf449-535e-4902-9138-6c7c104cca0d"
                                    :email "newuser@andrewslai.com"
                                    :role_id 2})
      (jdbc/query the-db "select * from users")))
  
  )

#_(def test-user-db
    (atom {:users [{:id 1
                    :username "Andrew"
                    :avatar (file->bytes (clojure.java.io/resource "avatars/happy_emoji.jpg"))}]
           :logins [{:id 1
                     :hashed_password (encryption/encrypt (encryption/make-encryption)
                                                          "Lai")}]}))




(def session-atom (atom {}))

(def components {:user (-> ptest/db-spec
                           postgres/->Postgres
                           users/->UserDatabase)
                 :session {:store (mem/memory-store session-atom)}})
(def test-users-app (h/wrap-middleware h/bare-app components))
(def identity-handler
  (h/wrap-middleware (api
                       (GET "/echo" request
                         {:user-authentication (:user request)})) components))

(deftest user-registration-test
  (let [b64-encoded-avatar (->> "Hello world!"
                                (map (comp byte int))
                                byte-array
                                b64/encode
                                String.)]
    (testing "Registration hapy path"
      (let [{:keys [status headers] :as response}
            (test-users-app (mock/request :post "/users"
                                          (json/generate-string
                                            {:username "new-user"
                                             :avatar b64-encoded-avatar
                                             :password "new-password"
                                             :first_name "new"
                                             :last_name "user"
                                             :email "newuser@andrewslai.com"})))
            user-url (get headers "Location")]
        (is (= 201 status))
        (is (= "/users/new-user" user-url))
        (is (= #{:id :username :avatar :first_name :last_name :email :role_id}
               (-> response
                   parse-response-body
                   keys
                   set)))
        (testing "Can retrieve the new user"
          (let [{:keys [status headers] :as response}
                (test-users-app (mock/request :get user-url))]
            (is (= 200 status))
            (is (= {:username "new-user"
                    :avatar b64-encoded-avatar
                    :first_name "new"
                    :last_name "user"
                    :email "newuser@andrewslai.com"
                    :role_id 2}
                   (parse-response-body response)))))))))


(deftest login-test
  (let [credentials (json/generate-string {:username "new-user", :password "new-password"})

        {:keys [status headers] :as initial-response}
        (test-users-app (mock/request :post "/login" credentials))

        cookie (first (get headers "Set-Cookie"))]
    (testing "login happy path"
      (is (= 200 status))
      (is (contains? headers "Set-Cookie")))
    (testing "cookies work properly"
      (let [{:keys [user-authentication]}
            (identity-handler (assoc-in (mock/request :get "/echo")
                                        [:headers "cookie"] cookie))]
        (is (= {:first_name "new",
                :last_name "user",
                :username "new-user",
                :email "newuser@andrewslai.com",
                :role_id 2,
                :avatar [72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33]}
               (dissoc (into {} user-authentication) :id)))))
    (testing "Can hit admin route with valid session token"
      (let [{:keys [status body]}
            (test-users-app (assoc-in (mock/request :get "/admin/")
                                      [:headers "cookie"] cookie))]
        (is (= 200 status))
        (is (= {:message "Got to the admin-route!"} (body->map body)))))
    (testing "Rejected from admin route when valid session token not present"
      (let [{:keys [status body]}
            (test-users-app (mock/request :get "/admin/"))]
        (is (= 401 status))
        (is (= "Not authorized" body))))
    (testing "After logout, cannot hit admin routes"
      (test-users-app (assoc-in (mock/request :post "/logout")
                                [:headers "cookie"] cookie))
      (let [{:keys [status body]}
            (test-users-app (assoc-in (mock/request :get "/admin/")
                                      [:headers "cookie"] cookie))]
        (is (= 401 status))
        (is (= "Not authorized" body)))))
  (testing "Login with incorrect password"
    (let [credentials (json/generate-string {:username "Andrew", :password "L"})
          {:keys [status headers]}
          (test-users-app (mock/request :post "/login" credentials))]
      (is (= 200 status))
      (is (not (contains? headers "Set-Cookie"))))))

(deftest user-avatar-test
  (testing "Get user avatar"
    (let [{:keys [status body headers]}
          (test-users-app (mock/request :get
                                        "/users/new-user/avatar"
                                        ))]
      (is (= 200 status))
      (is (= java.io.BufferedInputStream (type body))))))


(deftest update-user-test
  (let [b64-encoded-avatar (->> "Hello world!"
                                (map (comp byte int))
                                byte-array
                                b64/encode
                                String.)]
    (testing "Registration hapy path"
      (let [{:keys [headers] :as response}
            (test-users-app (mock/request :post "/users"
                                          (json/generate-string
                                            {:username "new-user"
                                             :avatar b64-encoded-avatar
                                             :password "new-password"
                                             :first_name "new"
                                             :last_name "user"
                                             :email "newuser@andrewslai.com"})))
            user-url (get headers "Location")]
        (testing "Can update the new user"
          (let [{:keys [status headers] :as response}
                (test-users-app (mock/request :patch user-url
                                              (json/generate-string
                                                {:username "new-user"
                                                 :first_name "new.2"
                                                 :last_name "user.2"})))]
            (is (= 200 status))
            (is (= {:first_name "new.2"
                    :last_name "user.2"}
                   (-> response
                       parse-response-body
                       (dissoc :id))))))
        (testing "Can retrieve the new user"
          (let [{:keys [status headers] :as response}
                (test-users-app (mock/request :get user-url))]
            (is (= 200 status))
            (is (= {:username "new-user"
                    :avatar b64-encoded-avatar
                    :first_name "new.2"
                    :last_name "user.2"
                    :email "newuser@andrewslai.com"
                    :role_id 2}
                   (parse-response-body response)))))))))
