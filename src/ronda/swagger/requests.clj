(ns ^:no-doc ronda.swagger.requests
  (:require [ronda.swagger
             [common :as common]
             [parameters :as parameters]
             [responses :as responses]]
            [ring.swagger.swagger2-schema :as sw]
            [schema.core :as s]
            [clojure.set :refer [rename-keys]]))

;; ## Data

(s/defschema SwaggerRequests
  {common/RequestMethod (s/maybe sw/Operation)})

(s/defschema SchemaPreprocessor
  (s/=> common/RequestSchema
        common/RequestSchema))

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
  [schema         :- common/RequestSchema
   request-method :- common/RequestMethod
   route-params   :- [s/Any]]
  (-> {:parameters (parameters/collect
                     schema
                     request-method
                     route-params)
       :responses  (responses/collect schema)}
      (merge (request-metadata schema))))

(s/defn ^:private add-swagger-request :- SwaggerRequests
  [result         :- SwaggerRequests
   schema         :- common/RequestSchema
   request-method :- s/Keyword
   route-params   :- s/Any]
  (->> (->swagger schema request-method route-params)
       (assoc result request-method)))

;; ## Request Schemas

(s/defn ^:private prepare-schemas :- common/Requests
  "Create a map of [method schema] pairs, expanding wildcards and
   multiple-method schemas."
  [requests :- common/Requests
   schema-preprocessor :- (s/maybe SchemaPreprocessor)]
  (let [process-fn (or schema-preprocessor identity)
        undefined-methods (common/undefined-methods requests)]
    (->> (for [[request-methods schema] requests
               request-method (common/seq-with-default
                                request-methods
                                undefined-methods)]
           [request-method (process-fn schema)])
         (into {}))))

(s/defn analyze :- SwaggerRequests
  "Analyze all request methods and create swagger-compliant
   data for further processing."
  ([requests route-params]
   (analyze requests route-params nil))
  ([requests            :- common/Requests
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
