(defproject ronda/swagger "0.1.0-SNAPSHOT"
  :description "swagger integration for ronda."
  :url "https://github.com/xsc/ronda-swagger"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [metosin/ring-swagger "0.20.1"]
                 [ronda/routing "0.2.7"]
                 [ronda/schema "0.1.1"]]
  :profiles {:dev {:dependencies [[midje "1.7.0-SNAPSHOT"]
                                  [ronda/routing-bidi "0.1.1"]
                                  [org.clojure/math.combinatorics "0.1.1"]
                                  [joda-time "2.7"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :codox {:plugins [[codox "0.8.11"]]
                     :codox {:project {:name "ronda/swagger"}
                             :src-dir-uri "https://github.com/xsc/ronda-swagger/blob/master/"
                             :src-linenum-anchor-prefix "L"
                             :defaults {:doc/format :markdown}}}}
  :aliases {"test" ["midje"]
            "codox" ["with-profile" "+codox" "doc"]}
  :pedantic? :abort)
