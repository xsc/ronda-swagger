(defproject ronda/swagger "0.1.5-SNAPSHOT"
  :description "swagger integration for ronda."
  :url "https://github.com/xsc/ronda-swagger"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [metosin/ring-swagger "0.22.3"]
                 [cheshire "5.5.0"]
                 [ronda/routing "0.2.8"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [ronda/routing-bidi "0.1.3"]
                                  [org.clojure/math.combinatorics "0.1.1"]
                                  [joda-time "2.9.1"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :codox {:plugins [[codox "0.8.11"]]
                     :codox {:project {:name "ronda/swagger"}
                             :src-dir-uri "https://github.com/xsc/ronda-swagger/blob/master/"
                             :src-linenum-anchor-prefix "L"
                             :defaults {:doc/format :markdown}}}}
  :aliases {"test" ["midje"]
            "codox" ["with-profile" "+codox" "doc"]}
  :pedantic? :abort)
