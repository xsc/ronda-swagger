(ns ronda.swagger.path-test
  (:require [midje.sweet :refer :all]
            [ronda.swagger.path :as path]
            [schema.core :as s]))

(s/with-fn-validation
  (tabular
    (fact "about path analysis."
          (let [r (path/analyze ?path)]
            (:route-params r) => ?route-params
            (:path r) => ?path-str))
    ?path                     ?path-str          ?route-params
    "/"                       "/"                []
    ["/" :id]                 "/:id"             [:id]
    ["/" [#"\d+" :id]]        "/:id"             [:id]
    ["/" [#"\d+" :id] "/go"]  "/:id/go"          [:id]))
