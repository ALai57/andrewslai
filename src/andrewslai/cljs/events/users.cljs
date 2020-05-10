(ns andrewslai.cljs.events.users
  (:require [ajax.core :refer [PATCH POST DELETE]]
            [andrewslai.cljs.events.core :refer [modify-db]]
            [andrewslai.cljs.utils :refer [image->blob]]
            [andrewslai.cljs.modal :refer [modal-template close-modal]]
            [re-frame.core :refer [dispatch reg-event-db]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db events for updating profile
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: Right now, we don't have a good way to know if the update was successful
;;       or not. Need to make the update endpoint send different status codes
;;       depending on whether the update was successful or not.
;;       Also need to update this event handler to handle both cases

;; TODO: Support avatar uploads in this function too. Right now it will be
;;       unhappy/unable to support conversion of an avatar image into a blob

(defn profile-update-failure [{:keys [message type] :as payload}]
  (let [feedback (get message :feedback)
        reasons (get message :data)]
    {:title "User update failed!"
     :body [:div {:style {:overflow-wrap "break-word"}}
            [:p [:b "Update unsuccessful."]]
            [:p type]
            [:br]
            [:p (str feedback)]
            [:br]
            [:p reasons]]
     :footer [:button {:type "button" :title "Ok"
                       :class "btn btn-default"
                       :on-click #(close-modal)} "Ok"]
     :close-fn close-modal}))

(defn process-profile-update-failure [db {:keys [response]}]
  (dispatch [:modal {:show? true
                     :child (modal-template (profile-update-failure response))
                     :size :small}])
  db)

(defn profile-update-success []
  {:title "User successfully updated!"
   :body [:div ]
   :footer [:button {:type "button" :title "Ok"
                     :class "btn btn-default"
                     :on-click #(close-modal)} "Ok"]
   :close-fn close-modal})

(defn process-profile-update [db {:keys [avatar_url] :as user}]
  (dispatch [:modal {:show? true
                     :child (modal-template (profile-update-success))
                     :size :small}])
  (let [now (.getTime (js/Date.))
        avatar_url {:avatar_url (str avatar_url "?" now)}]
    (assoc db :user (merge (:user db) user avatar_url))))

(reg-event-db
  :update-profile
  (fn [db [_ {:keys [username] :as request}]]

    (PATCH (str "/users/" username)
        {:params request
         :format :json
         :handler #(dispatch [:process-http-response % process-profile-update])
         :error-handler #(dispatch [:process-http-response % process-profile-update-failure])})

    db))

(def IllegalArgumentEx :andrewslai.clj.persistence.users/IllegalArgumentException)
(def PSQLEx :andrewslai.clj.persistence.users/PSQLException)

(defn delete-success [username]
  {:title "User successfully deleted!"
   :body [:div
          [:br]
          [:div [:p [:b "Username: "] username]]]
   :footer [:button {:type "button" :title "Ok"
                     :class "btn btn-default"
                     :on-click #(close-modal)} "Ok"]
   :close-fn close-modal})
(defn user-deleted [db [_ username]]
  (dispatch [:modal {:show? true
                     :child (modal-template (delete-success username))
                     :size :small}])
  (assoc db :user nil))
(reg-event-db
  :user-deleted
  user-deleted)

(defn delete-failure [username]
  {:title "Unable to delete user"
   :body [:div {:style {:overflow-wrap "break-word"}}
          [:p [:b "Delete operation unsuccessful."]]
          [:br]
          [:p "User:" username]]
   :footer [:button {:type "button" :title "Ok"
                     :class "btn btn-default"
                     :on-click #(close-modal)} "Ok"]
   :close-fn close-modal})
(defn user-delete-failed [db username]
  (dispatch [:modal {:show? true
                     :child (modal-template (delete-failure username))
                     :size :small}])
  db)
(reg-event-db
  :user-delete-failed
  user-delete-failed)
