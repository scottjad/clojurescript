(defproject browser-repl "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [compojure "0.6.5"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler repl.endpoint/endpoint}
  :source-path "src/clj")