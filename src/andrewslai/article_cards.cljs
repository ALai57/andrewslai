(ns andrewslai.article-cards
  (:require [reagent.core  :as reagent]
            [clojure.string :as str]
            [re-frame.core :refer [subscribe
                                   dispatch
                                   reg-sub]]
            [cljsjs.react-bootstrap]))


(def Card (reagent/adapt-react-class (aget js/ReactBootstrap "Card")))

(defn article-tags->icon [article-tags]
  (condp = article-tags
    "research" "images/nav-bar/neuron-icon.svg"
    "archive" "images/nav-bar/archive-icon.svg"
    "about" "images/nav-bar/andrew-silhouette-icon.svg"
    "thoughts" "images/nav-bar/andrew-head-icon.svg"))

(defn make-card
  [{:keys [article_tags title article_url article_id] :as article}]

  ^{:key article_id}
  [Card {:class "text-white bg-light mb-3 article-card"}
   [:div.container-fluid
    [:div.row.flex-items-xs-middle
     [:div.col-sm-3.bg-primary.text-xs-center.card-icon
      [:div.p-y-3
       [:h1.p-y-2
        [:img.fa.fa-2x {:src (article-tags->icon article_tags)
                        :style {:width "100%"}}]]]]
     [:div.col-sm-9.bg-light.text-dark.card-description
      [:h5.card-title>a {:href (str "#/" article_tags
                                    "/content/" article_url)}
       title]
      [:p.card-text article_url]]]]])

(defn recent-content-display
  [content-type]
  (let [recent-content (subscribe [:recent-content])
        the-content (if content-type
                      (filter #(= (:article_tags %1) content-type)
                              @recent-content)
                      @recent-content)]
    [:div#recent-content
     [:div#recent-article-cards.card-group
      (map make-card the-content)]]))
