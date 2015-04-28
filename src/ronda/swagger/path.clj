(ns ^:no-doc ronda.swagger.path
  (:require [schema.core :as s]))

(defn- add-route-param
  [m k]
  (-> m
      (update-in [:route-params] conj k)
      (update-in [:path] str ":" (name k))))

(defn- add-segment
  [m s]
  (update-in m [:path] str s))

(s/defn analyze :- {:route-params [s/Any]
                    :path s/Str}
  "Given a bidi-style path vector (or string), return a map
   of `:route-params` (the route params) available in the path
   and `:path` (the path as a string)."
  [path :- (s/either s/Str [s/Any])]
  (if (string? path)
    (recur [path])
    (reduce
      (fn [m element]
        (cond (vector? element) (add-route-param m (last element))
              (keyword? element) (add-route-param m element)
              :else (add-segment m element)))
      {:route-params [], :path ""}
      path)))
