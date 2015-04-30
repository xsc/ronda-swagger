(ns ^:no-doc ronda.swagger.descriptor
  (:require [ronda.swagger
             [common :as common]
             [path :as path]
             [requests :as requests]]
            [ronda.routing
             [descriptor :refer [RouteDescriptor routes]]
             [middleware-data :as md]]
            [ring.swagger.swagger2-schema :as sw]
            [schema.core :as s]))

;; ## Metadata
;;
;; Swagger Data can be attached to the `:swagger` middleware key. If it is
;; `false`, the route is ignored. If it is a map, the data will be merged into
;; each schema.

(defn- get-schema
  [route-data]
  (if-not (md/route-middleware-disabled? route-data :schema)
    (let [schema (md/route-middleware-data route-data :schema)]
      (if (empty? schema)
        (assoc schema :get {})
        schema))
    {:get {}}))

(defn- get-swagger
  [route-data]
  (when-not (md/route-middleware-disabled? route-data :swagger)
    (let [data (md/route-middleware-data route-data :swagger)]
      (or (nil? data) data))))

(s/defn ^:private route-schemas :- (s/maybe common/Requests)
  "Given route data, read the route schemas (from the `:schema` middleware
   key), heeding the value of the `:swagger` middleware key."
  [route-data]
  (when-let [swagger (get-swagger route-data)]
    (let [schema (get-schema route-data)]
      (if (map? swagger)
        (->> (for [[method s] schema]
               [method (merge swagger s)])
             (into {}))
        schema))))

(s/defn ^:private route-path :- {:route-params [s/Any], :path-str s/Str}
  [{:keys [path]}]
  (let [{:keys [path route-params]} (path/analyze path)]
    {:route-params (vec (distinct route-params))
     :path-str (str path)}))

(s/defn disable-swagger :- (s/protocol RouteDescriptor)
  "Disable swagger for the given route IDs."
  [descriptor :- (s/protocol RouteDescriptor)
   route-ids :- [s/Any]]
  (reduce
    #(md/disable-middlewares %1 %2 [:swagger])
    descriptor
    route-ids))

;; ## Analysis

(s/defschema SwaggerPath
  {:path s/Str
   :schema requests/SwaggerRequests})

(s/defschema RouteData
  {:path s/Any
   (s/optional-key :meta) {s/Any s/Any}
   s/Any s/Any})

(s/defschema RouteSchemaPreprocessor
  (s/=> common/RequestSchema
        RouteData
        common/RequestSchema))

(s/defn analyze-route :- (s/maybe SwaggerPath)
  "Analyze a single piece of route data and produce a map of `:path` and
   `:schema` for further processing."
  ([route-data] (analyze-route route-data nil))
  ([route-data          :- RouteData
    schema-preprocessor :- (s/maybe RouteSchemaPreprocessor)]
   (when-let [requests (route-schemas route-data)]
     (let [{:keys [route-params path-str] :as path-data} (route-path route-data)
           preproc (when schema-preprocessor
                     #(schema-preprocessor route-data %))]
       {:schema (requests/analyze requests route-params preproc)
        :path   path-str}))))

(s/defn analyze :- [SwaggerPath]
  "Analyze all routes represented by the given RouteDescriptor."
  ([descriptor] (analyze descriptor nil))
  ([descriptor          :- (s/protocol RouteDescriptor)
    schema-preprocessor :- (s/maybe requests/SchemaPreprocessor)]
   (keep
     (fn [[route-id route-data]]
       (analyze-route route-data schema-preprocessor))
     (routes descriptor))))
