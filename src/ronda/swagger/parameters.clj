(ns ^:no-doc ronda.swagger.parameters
  (:require [ronda.swagger.common :as common]
            [ring.swagger.swagger2-schema :as sw]
            [schema.core :as s]))

(s/defn ^:private route-param-predicate :- (s/=> s/Bool s/Any)
  "Generate predicate checking whether a given key belongs to a
   route param."
  [route-params :- [s/Any]]
  (let [route-param-set (set route-params)]
    (fn [k]
      (contains? route-param-set (s/explicit-schema-key k)))))

(s/defn ^:private default-parameter-type :- s/Keyword
  "Compute the default parameter type."
  [{:keys [body]} :- common/RequestSchema
   request-method :- s/Keyword]
  (if (or (#{:get :head :options} request-method)
          body)
    :query
    :formData))

(s/defn ^:private add-missing-route-params
  "Make sure all route params are available in the schema, adding them
   with type 'string' if missing."
  [route-params :- [s/Any]
   params       :- (s/maybe {s/Any s/Any})]
  (let [exists? (set (map s/explicit-schema-key (keys params)))
        missing (remove exists? route-params)]
    (merge
      (zipmap missing (repeat s/Str))
      params)))

(s/defn collect :- sw/Parameters
  "Collect all parameters from the given schema.

   - if the method is GET/HEAD/OPTIONS, `:params` will be interpreted as
     query parameters,
   - if a body is given, `:params` will be interpreted as query parameters,
   - otherwise, `:params` will be interpreted as form parameter.

   `:headers` and `:body` will be used directly."
  [schema         :- common/RequestSchema
   request-method :- s/Keyword
   route-params   :- [s/Any]]
  (let [route-param? (route-param-predicate route-params)
        default-type (default-parameter-type schema request-method)
        {:keys [headers body params]} schema
        base (cond-> {}
               headers (assoc :header headers)
               body    (assoc :body body))]
    (->> params
         (add-missing-route-params route-params)
         (reduce
           (fn [result [param-key param-schema]]
             (let [k (if (route-param? param-key)
                       :path
                       default-type)]
               (assoc-in result [k param-key] param-schema)))
           base))))
