(ns ronda.swagger.requests-test
  (:require [midje.sweet :refer :all]
            [ronda.swagger.requests :as requests]
            [schema.core :as s]))

(s/with-fn-validation
  (fact "about simple request analysis."
        (let [r (requests/analyze
                  {:get {:params {:id s/Int}}}
                  [:id])]
          (-> r :get :parameters :path) => {:id s/Int})))

(s/with-fn-validation
  (fact "about metadata values."
        (let [r (requests/analyze
                  {:post {:params {:id s/Int, :value s/Str}
                          :description "request"
                          :deprecated? true
                          :external-docs "http://localhost"
                          :tags [:tag]
                          :id :performSomething
                          :x-custom-value "val"}}
                  [:id])
              {:keys [description parameters deprecated
                      externalDocs operationId tags
                      x-custom-value]} (:post r)]
          (:path parameters) => {:id s/Int}
          (:formData parameters) => {:value s/Str}
          description => "request"
          deprecated => true
          externalDocs => "http://localhost"
          operationId => :performSomething
          tags => [:tag]
          x-custom-value => "val")))

(s/with-fn-validation
  (fact "about multi-request-method schemas."
        (let [r (requests/analyze
                  {:get {:params {:id s/Int}}
                   [:post :put] {:params {:id s/Str}}}
                  [])]
          (-> r :get :parameters :query) => {:id s/Int}
          (-> r :post :parameters :formData) => {:id s/Str}
          (-> r :put :parameters :formData) => {:id s/Str})))

(s/with-fn-validation
  (fact "about request method wildcards."
        (let [r (requests/analyze
                  {:get {:params {:id s/Int}}
                   :*   {:params {:id s/Str}}}
                  [])]
          (-> r :get :parameters :query) => {:id s/Int}
          (-> r :head :parameters :query) => {:id s/Str}
          (-> r :options :parameters :query) => {:id s/Str}
          (-> r :post :parameters :formData) => {:id s/Str}
          (-> r :put :parameters :formData) => {:id s/Str}
          (-> r :delete :parameters :formData) => {:id s/Str}
          (-> r :patch :parameters :formData) => {:id s/Str})))

(s/with-fn-validation
  (fact "about schema preprocessors."
        (let [r (requests/analyze
                  {:get {:params {:id s/Int}}
                   :post {:params {:id s/Int}}}
                  []
                  #(assoc-in % [:params :auth-token] s/Str))]
          (-> r :get :parameters :query) => {:id s/Int, :auth-token s/Str}
          (-> r :post :parameters :formData) => {:id s/Int, :auth-token s/Str})))
