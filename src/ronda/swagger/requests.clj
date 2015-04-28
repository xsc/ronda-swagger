(ns ronda.swagger.requests
  (:require [ronda.swagger
             [parameters :as parameters]
             [responses :as responses]]
            [ronda.schema.data
             [common :as common]
             [request :as rq]]
            [ring.swagger.swagger2-schema :as sw]
            [schema.core :as s]
            [clojure.set :refer [rename-keys]]))

;; ## Data

(def ^:private valid-request-methods
  #{:get :post :put :delete :head :options :patch})

(s/defschema RequestMethod
  (apply s/enum valid-request-methods))

(s/defschema SwaggerRequests
  {RequestMethod (s/maybe sw/Operation)})

(s/defschema SchemaPreprocessor
  (s/=> rq/RawRequestSchema rq/RawRequestSchema))

;; ## Schema Collection

(defn- request-metadata
  [schema]
  (let [x-ks (filter
               (comp #(.startsWith ^String % "x-") name)
               (keys schema))]
    (-> schema
        (select-keys
          (concat
            [:description :summary :tags
             :consumes :produces :schemes
             :external-docs :deprecated?
             :id]
            x-ks))
        (rename-keys
          {:deprecated?   :deprecated
           :external-docs :externalDocs
           :id            :operationId}))))

(s/defn ^:private ->swagger
  [schema :- rq/RawRequestSchema
   request-method :- RequestMethod
   route-params :- [s/Any]]
  (-> {:parameters (parameters/collect
                     schema
                     request-method
                     route-params)
       :responses  (responses/collect schema)}
      (merge (request-metadata schema))))

(s/defn ^:private add-swagger-request :- SwaggerRequests
  [result         :- SwaggerRequests
   schema         :- rq/RawRequestSchema
   request-method :- s/Keyword
   route-params   :- s/Any]
  (->> (->swagger schema request-method route-params)
       (assoc result request-method)))

;; ## Request Schemas

(s/defn ^:private prepare-schemas
  :- {RequestMethod rq/RawRequestSchema}
  "Create a map of [method schema] pairs, expanding wildcards and
   multiple-method schemas."
  [requests :- rq/RawRequests
   schema-preprocessor :- (s/maybe SchemaPreprocessor)]
  (let [process-fn (or schema-preprocessor identity)
        undefined-methods (remove
                            (set (mapcat common/as-seq (keys requests)))
                            valid-request-methods)]
    (->> (for [[request-methods schema] requests
               request-method (if (common/wildcard? request-methods)
                                undefined-methods
                                (common/as-seq request-methods))]
           [request-method (process-fn schema)])
         (into {}))))

(s/defn analyze :- SwaggerRequests
  "Analyze all request methods and create swagger-compliant
   data for further processing."
  ([requests route-params]
   (analyze requests route-params nil))
  ([requests :- rq/RawRequests
    route-params        :- [s/Any]
    schema-preprocessor :- (s/maybe SchemaPreprocessor)]
   (let [schemas (prepare-schemas requests schema-preprocessor)]
     (reduce
       (fn [result [request-method schema]]
         (add-swagger-request
           result
           schema
           request-method
           route-params))
       {} schemas))))
