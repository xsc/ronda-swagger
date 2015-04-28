(defproject ronda/swagger "0.1.0-SNAPSHOT"
  :description "swagger integration for ronda."
  :url "https://github.com/xsc/ronda-swagger"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [metosin/ring-swagger "0.20.1"]
                 [ronda/routing "0.2.6"]
                 [ronda/schema "0.1.1"]
                 [cheshire "5.4.0"]]
  :profiles {:codox {:plugins [[codox "0.8.10"]]
                     :codox {:project {:name "ronda/swagger"}
                             :src-dir-uri "https://github.com/xsc/ronda-swagger/blob/master/"
                             :src-linenum-anchor-prefix "L"
                             :defaults {:doc/format :markdown}}}}
  :aliases {"codox" ["with-profile" "+codox" "doc"]}
  :pedantic? :abort)
