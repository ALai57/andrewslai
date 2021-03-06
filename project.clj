(defproject andrewslai "0.0.2"
  :description "Template for full stack development in Clojure"
  :dependencies [[buddy/buddy-auth "2.2.0"]
                 [cheshire "5.10.0"]
                 [clj-commons/secretary "1.2.4"]
                 [clj-http "3.10.0"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/react "16.13.0-0"]
                 [cljsjs/react-dom "16.13.0-0"]
                 [cljsjs/react-bootstrap "1.0.0-beta.14-0"] ;; latest release
                 [cljsjs/react-pose "1.6.4-1"]
                 [cljsjs/slate "0.33.6-0"]
                 [cljsjs/slate-react "0.12.6-0"]
                 [cljsjs/slate-html-serializer "0.6.3-0"]
                 [cljsjs/zxcvbn "4.4.0-1"]
                 [com.nulab-inc/zxcvbn "1.3.0"]
                 [org.slf4j/slf4j-nop "1.7.30"]
                 [io.zonky.test/embedded-postgres "1.2.6" :scope "test"]
                 [day8.re-frame/tracing "0.5.3"]
                 [hiccup "1.0.5"]
                 [hickory "0.7.1"]
                 [honeysql "0.9.10"]
                 [http-kit "2.3.0"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [metosin/spec-tools "0.10.3"]
                 [migratus "1.2.8"]
                 [nubank/matcher-combinators "3.1.4"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "1.0.567"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.clojure/java.data "1.0.64"]
                 [org.keycloak/keycloak-adapter-core "12.0.3"]
                 [org.keycloak/keycloak-adapter-spi "12.0.3"]
                 [org.keycloak/keycloak-admin-client "12.0.3"]
                 [org.keycloak/keycloak-core "12.0.3"]
                 [org.keycloak/keycloak-common "12.0.3"]
                 [org.postgresql/postgresql "42.2.11"]
                 [re-frame "0.12.0"]
                 [reagent "0.10.0"]
                 [ring "1.8.0"]
                 [ring/ring-mock "0.4.0"]
                 [sablono "0.8.6"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [slingshot "0.12.2"]
                 [com.taoensso/timbre "4.10.0"]]

  :plugins [[lein-doo "0.1.10"]]

  ;; Used to make this compatible with Java 11
  :managed-dependencies
  [[metosin/ring-swagger-ui "3.25.3"]
   [org.clojure/core.rrb-vector "0.1.1"]
   [org.flatland/ordered "1.5.7"]
   [io.zonky.test.postgres/embedded-postgres-binaries-linux-amd64-alpine "10.6.0" :scope "test"]
   [io.zonky.test.postgres/embedded-postgres-binaries-linux-amd64 "10.6.0" :scope "test"]]

  :aot :all
  :uberjar-name "andrewslai.jar"
  :main andrewslai.clj.handler

  :clean-targets ^{:protect false} [:target-path "resources/public/js/compiled"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
            "fig:prod"  ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "uberjar"]}

  :profiles
  {:dev {:dependencies [[binaryage/devtools "1.0.0"]
                        [cider/piggieback "0.4.2"]
                        [figwheel-sidecar "0.5.19"]
                        [org.clojure/test.check "1.1.0"]
                        [com.bhauman/figwheel-main "0.2.12"]
                        [com.bhauman/rebel-readline-cljs "0.1.4"]
                        [lein-doo "0.1.10"]]
         :plugins [[lein-ancient "0.6.15"]
                   [lein-kibit "0.1.8"]
                   [lein-ring "0.12.5"]]
         :source-paths ["src/andrewslai/cljs"]
         :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
         :aliases {"migratus" ["run" "-m" andrewslai.clj.persistence.migrations]}}

   :uberjar {:dependencies [[com.bhauman/figwheel-main "0.2.12"]]
             :source-paths ["src/andrewslai/cljs"]
             :prep-tasks ["compile" "fig:prod"]}})
