(ns ronda.swagger.parameters-test
  (:require [midje.sweet :refer :all]
            [ronda.swagger.parameters :as parameters]
            [schema.core :as s]))

;; ## Fixtures

(def test-schema
  {:params {:id s/Int}})

(def test-schema-with-body
  (assoc test-schema :body {:value s/Str}))

;; ## Tests

(s/with-fn-validation
  (tabular
    (fact "about default parameter types."
          (let [r (parameters/collect ?schema ?method nil)]
            (get r ?k) => {:id s/Int}))
    ?schema               ?method  ?k
    test-schema           :get     :query
    test-schema           :head    :query
    test-schema           :options :query
    test-schema           :post    :formData
    test-schema           :delete  :formData
    test-schema-with-body :get     :query
    test-schema-with-body :post    :query))

(s/with-fn-validation
  (fact "about body params."
        (let [r (parameters/collect
                  test-schema-with-body
                  :post
                  [])]
          (:body r) => {:value s/Str}
          (:query r) => {:id s/Int})))

(s/with-fn-validation
  (fact "about route param detection."
        (let [r (parameters/collect test-schema :get [:id])]
          (:query r) => falsey
          (:path r) => {:id s/Int})))

(s/with-fn-validation
  (fact "about missing route params."
        (let [r (parameters/collect test-schema :get [:auth-token])]
          (:query r) => {:id s/Int}
          (:path r) => {:auth-token s/Str})))
