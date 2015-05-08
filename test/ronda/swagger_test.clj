(ns ronda.swagger-test
  (:require [midje.sweet :refer :all]
            [ronda.swagger :refer :all]
            [ronda.routing.bidi :as bidi]
            [ronda.routing.middleware-data :as md]
            [schema.core :as s]))

;; ## Data

(s/defschema Body {:text s/Str})

(def test-descriptor
  (-> (bidi/descriptor
        ["/" {""           :root
              ["doc/" :id] {:get :doc
                            :put :create}}])
      (md/enable-middlewares
        :doc    {:schema {:get {:params {:id s/Int}}}
                 :auth   true}
        :create  {:schema {:put {:params {:id s/Int}
                                 :body Body}}
                  :auth   true})))

(defn conditional-auth-param
  [route-data schema]
  (if (md/route-middleware-enabled? route-data :auth)
    (assoc-in schema [:params :auth-token] s/Str)
    schema))

;; ## Tests

(s/with-fn-validation
  (fact "about the swag."
        (let [result (swagger-json
                       test-descriptor
                       {:schema-preprocessor conditional-auth-param
                        :info {:title "ronda API"
                               :version "v1"}
                        :consumes ["application/json"
                                   "application/x-www-form-urlencoded"]})
              paths (:paths result)]
          (:swagger result) => "2.0"
          (:info result) => {:title "ronda API", :version "v1"}
          (:produces result) => ["application/json"]
          (-> result :consumes count) => 2
          (-> paths keys set) => #{"/" "/doc/{id}"}
          (-> (paths "/doc/{id}") keys set) => #{:get :put}
          (->> (paths "/doc/{id}")
               :get :parameters
               (map (juxt :name :in :type))
               (set))
          => #{["auth-token" :query "string"]
               ["id" :path "integer"]}
          (->> (paths "/doc/{id}")
               :put :parameters
               (map (juxt :name :in :type))
               (set))
          => #{["Body" :body nil]
               ["auth-token" :query "string"]
               ["id" :path "integer"]})))

(s/with-fn-validation
  (fact "about the swaggy response."
        (let [{:keys [status headers body]} (swagger-json-response test-descriptor)]
          status => 200
          (headers "content-type") => #"^application/json.*"
          body => map?)
        (swagger-json-response nil) => nil?))

(s/with-fn-validation
  (fact "about the handler's swag."
        (let [h (swagger-handler)
              {:keys [status headers body]}
              (h {:request-method :get
                  :uri "/swagger.json"
                  :ronda/descriptor test-descriptor})]
          status => 200
          (headers "content-type") => #"^application/json.*"
          body => string?)))
