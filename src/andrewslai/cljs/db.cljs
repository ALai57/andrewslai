(ns andrewslai.cljs.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))


;; -- Spec --------------------------------------------------------------------
;; The value in app-db should always match this spec.

(s/def ::active-panel keyword?)
(s/def ::loading boolean?)
(s/def ::db
  (s/keys :rEeq-un [::active-panel
                    ::active-content
                    ::loading?]))

;; -- Default app-db Value  ---------------------------------------------------

(def default-db
  {:active-panel :home
   :active-content nil
   :recent-content nil
   :loading? false
   :radial-menu-open? true
   :active-icon [:me {:image-url "images/my-silhouette.svg"}]
   :github-data nil
   :circles [{:name "circle 1"
              :x 10
              :y 10
              :r 10
              :color "black"}
             {:name "circle 2"
              :x 35
              :y 35
              :r 15
              :color "red"}
             {:name "circle 3"
              :x 100
              :y 100
              :r 30
              :color "blue"}]})