(ns andrewslai.clj.routes.portfolio
  (:require [andrewslai.clj.api.portfolio :as portfolio-api]
            [compojure.api.sweet :refer [context defroutes GET]]
            [ring.util.http-response :refer [ok]]))

(defroutes portfolio-routes
  (context "/projects-portfolio" []
    :tags ["projects-portfolio"]
    :components [database]
    (GET "/" []
      (ok (portfolio-api/get-portfolio database)))))
