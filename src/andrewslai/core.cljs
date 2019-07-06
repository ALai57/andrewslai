(ns andrewslai.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary]
            [andrewslai.events] ;; These two are only required to make the compiler
            [andrewslai.subs]   ;; load them (see docs/App-Structure.md)
            [andrewslai.views]
            [devtools.core :as devtools])
  (:import [goog History]
           [goog.history EventType]))


;; -- Debugging aids ----------------------------------------------------------
(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)   ;; so that println writes to `console.log`


;; Put an initial value into app-db.
;; The event handler for `:initialise-db` can be found in `events.cljs`
;; Using the sync version of dispatch means that value is in
;; place before we go onto the next step.
(dispatch-sync [:initialise-db])

;; -- Routes and History ------------------------------------------------------

(defroute "/" [] (dispatch [:set-active-panel :home]))
(defroute "/:path" [path] (dispatch [:set-active-panel (keyword path)]))
(defroute "/:path/content/:content-name" [path content-name]
  (dispatch [:retrieve-content (keyword path) (keyword content-name)]))


(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; -- Entry Point -------------------------------------------------------------
;; Within ../../resources/public/index.html you'll see this code
;;    window.onload = function () {
;;      andrewslai.core.main();
;;    }
;; So this is the entry function that kicks off the app once HTML is loaded

(defn ^:export main
  []
  ;; `andrewslai.views/app` is the root view for the entire UI.
  (reagent/render [andrewslai.views/app]
                  (.getElementById js/document "app")))