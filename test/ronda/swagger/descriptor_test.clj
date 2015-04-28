(ns ronda.swagger.descriptor-test
  (:require [midje.sweet :refer :all]
            [ronda.swagger.descriptor :as d]
            [ronda.routing.bidi :as bidi]
            [ronda.routing.middleware-data :as md]
            [schema.core :as s]))

;; ## Route Data Tests

(s/with-fn-validation
  (tabular
    (fact "about simple route analysis."
          (let [r (d/analyze-route
                    {:path ?path
                     :meta {:middlewares
                            {:schema {:get ?schema}
                             :swagger true}}})]
            (-> r :schema keys) => [:get]
            (:path r) => ?path-str
            (-> r :schema :get :parameters) => ?parameters))
    ?path             ?schema               ?path-str     ?parameters
    "/"               {}                    "/"           {}
    ["/" :id]         {}                    "/:id"        {:path {:id s/Str}}
    ["/" :id]         {:params {:id s/Int}} "/:id"        {:path {:id s/Int}}
    ["/" :id]         {:params {:x s/Int}}  "/:id"        {:path {:id s/Str}
                                                           :query {:x s/Int}}
    ["/" [#"\d" :id]] {:params {:x s/Int}}  "/:id"        {:path {:id s/Str}
                                                           :query {:x s/Int}}))

(s/with-fn-validation
  (fact "about swagger metadata in route data."
        (let [r (d/analyze-route
                  {:path "/"
                   :meta {:middlewares
                          {:schema {:get {}
                                    :post {}}
                           :swagger {:tags [:user]}}}})]
          (-> r :schema :get :tags) => [:user]
          (-> r :schema :post :tags) => [:user])))

(s/with-fn-validation
  (fact "about schema preprocessing."
        (let [r (d/analyze-route
                  {:path "/"
                   :meta {:middlewares
                          {:schema {:get {}
                                    :post {}}
                           :swagger {:tags [:user]}}}}
                  #(assoc-in %2 [:params :auth-token] s/Str))]
          (-> r :schema :get :parameters :query) => {:auth-token s/Str}
          (-> r :schema :post :parameters :formData) => {:auth-token s/Str})))

(s/with-fn-validation
  (fact "about ignored routes."
        (d/analyze-route
          {:path "/"
           :meta {:middlewares
                  {:schema {:get {}}
                   :swagger false}}}) => falsey))

;; ## Descriptor Tests

(def test-descriptor
  (-> (bidi/descriptor
        ["/" {""           :root
              ["doc/" :id] :doc}])
      (md/enable-middlewares
        :doc  {:schema {:get {:params {:id s/Int}}}
               :auth   true})))

(defn conditional-auth-param
  [route-data schema]
  (if (md/route-middleware-enabled? route-data :auth)
    (assoc-in schema [:params :auth-token] s/Str)
    schema))

(s/with-fn-validation
  (fact "about descriptor analysis."
        (let [r (->> (d/analyze test-descriptor conditional-auth-param)
                     (map (juxt :path :schema))
                     (into {}))]
          (count r) => 2
          (-> (r "/") :get :parameters) => {}
          (-> (r "/doc/:id") :get :parameters)
          => {:path {:id s/Int}
              :query {:auth-token s/Str}})))
