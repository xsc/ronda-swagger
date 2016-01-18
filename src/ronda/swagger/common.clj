(ns ronda.swagger.common
  (:require [schema.core :as s]))

;; ## Seq Helpers

(defn as-seq
  "Convert value to seq if it's not already."
  [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn seq-with-default
  "Convert value to seq. If it contains the wildcard value `:*` return
   the default value."
  [v default]
  (let [sq (as-seq v)]
    (if (some #{:*} sq)
      (as-seq default)
      sq)))

;; ## Schema Helpers

(s/defn optional-keys
  "Make all schema keys optional."
  [m :- {s/Any s/Any}]
  (->> (for [[k v] m]
         [(s/optional-key k) v])
       (into {})))

;; ## Schemas

(def valid-request-methods
  "All valid request methods."
  #{:get :post :put :delete :head :options :patch})

(s/defschema RequestMethod
  (apply s/enum (cons :* valid-request-methods)))

(s/defschema ResponseSchema
  "Subset of ronda-schema's response schema."
  (merge
    (optional-keys
      {:headers {s/Any s/Any}
       :body    s/Any})
    {s/Any s/Any}))

(s/defschema RequestSchema
  "Subset of ronda-schema's request schema."
  (merge
    (optional-keys
      {:params    {s/Any s/Any}
       :headers   {s/Any s/Any}
       :body      s/Any
       :responses {s/Int ResponseSchema}})
    {s/Any s/Any}))

(s/defschema Requests
  "Schema for a collection of request schemas."
  {(s/either
     RequestMethod
     [RequestMethod])
   RequestSchema})

;; ## Methods

(s/defn undefined-methods :- [RequestMethod]
  "Find all request methods that are not defined within the given
   request schemas."
  [requests :- Requests]
  (remove
    (set (mapcat as-seq (keys requests)))
    valid-request-methods))

;; ## Headers Maps

(s/defn explicit-headers :- {s/Any s/Any}
  [headers :- (s/maybe {s/Any s/Any})]
  (->> (for [[k v] headers]
         [(cond-> k (string? k) s/required-key) v])
       (into {})))
