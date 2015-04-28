(ns ronda.swagger
  (:require [ronda.swagger.descriptor :as descriptor]
            [ronda.routing :as routing]
            [ring.swagger.swagger2 :as sw2]))

;; ## Swagger JSON

(defn- attach-tags
  [{:keys [tags] :as swagger-data} paths]
  swagger-data)

(defn- attach-paths
  [swagger-data paths]
  (reduce
    (fn [swagger-data {:keys [path schema]}]
      (assoc-in swagger-data [:paths path] schema))
    swagger-data paths))

(defn- initial-swagger-data
  [options]
  (dissoc
    options
    :schema-preprocessor
    :ignore-missing-mappings?
    :default-response-description-fn))

(defn- swagger-opts
  [options]
  (select-keys
    options
    [:ignore-missing-mappings?
     :default-response-description-fn]))

(defn swagger-json
  "Create a swagger-compliant Clojure map.

   The following `options` will be passed to ring-swagger's generator
   function:

   - `:ignore-missing-mappings?`
   - `:default-response-description-fn`

   The following `options` will be used for processing ronda's schemas:

   - `:schema-preprocessor`

   Everything else will be passed directly as data to ring-swagger's
   generator and thus be included in the swagger result."
  [descriptor
   & [{:keys [schema-preprocessor
              ignore-missing-mappings?
              default-response-description-fn]
       :or {ignore-missing-mappings?        false
            default-response-description-fn (constantly "")}
       :as options}]]
  (let [paths (descriptor/analyze descriptor schema-preprocessor)
        opts (swagger-opts options)]
    (-> (initial-swagger-data options)
        (attach-tags paths)
        (attach-paths paths)
        (sw2/swagger-json opts))))

;; ## Response

(defn swagger-json-response
  "Create a ring-compliant JSON swagger response for the given
   descriptor."
  [descriptor & [options]]
  (when descriptor
    {:status 200
     :headers {"content-type" "application/json;charset=utf-8"}
     :body (swagger-json descriptor options)}))

;; ## Handler

(defn swagger-handler
  "Create a ring-compliant handler that reads the ronda RouteDescriptor
   from the incoming request and produces a swagger JSON response.

   - `:memoize?`: whether to memoize the swagger body,
   - `:encode`: function to use to encode the swagger Clojure map.

   See `swagger-json` for more possible values in `options`."
  [& [{:keys [memoize? encode]
       :or {memoize? true
            encode   identity}
       :as options}]]
  (let [swagger-opts (dissoc options :memoize? :encode)
        response-for (fn [request]
                       (some-> (swagger-json-response request swagger-opts)
                               (update-in [:body] #(some-> % encode))))
        response-for (if memoize?
                       (memoize response-for)
                       response-for)]
    (fn [request]
      (response-for
        (routing/descriptor request)))))
