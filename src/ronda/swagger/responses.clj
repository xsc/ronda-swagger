(ns ^:no-doc ronda.swagger.responses
  (:require [ronda.swagger.common :as common]
            [schema.core :as s]
            [clojure.set :refer [rename-keys]]))

(s/defn collect
  [{:keys [responses]} :- common/RequestSchema]
  (if-not (empty? responses)
    (->> (for [[statuses schema] responses
               status (common/seq-with-default statuses [:default])]
           (->> (-> schema
                    (select-keys [:body :description :examples :headers])
                    (update-in [:headers] common/explicit-headers)
                    (rename-keys {:body :schema}))
                (vector status)))
         (into {}))
    {}))
