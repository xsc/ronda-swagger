(ns ronda.swagger.responses
  (:require [ronda.schema.data
             [common :as common]
             [request :as rq]
             [response :as rs]]
            [schema.core :as s]
            [clojure.set :refer [rename-keys]]))

(s/defn collect
  [{:keys [responses]} :- rq/RawRequestSchema]
  (if-not (empty? responses)
    (->> (for [[statuses schema] responses
               status (if (common/wildcard? statuses)
                        [:default]
                        (common/as-seq statuses))]
           (->> (-> schema
                    (select-keys [:body :description :examples :headers])
                    (rename-keys {:body :schema}))
                (vector status)))
         (into {}))
    {}))
