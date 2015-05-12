## Quickstart

This guide incidentally offers a (very coarse) introduction into
[ronda-routing][ronda-routing] and [ronda-schema][ronda-schema] - but you should
have a closer look at both projects to leverage their full power.

[ronda-routing]: https://github.com/xsc/ronda-routing
[ronda-schema]: https://github.com/xsc/ronda-schema

### Step 1: Routes

The main data structure offered by ronda is the `RouteDescriptor` which
represents all available paths and - optionally - additional metadata. It can be
created using e.g. [ronda-routing-bidi][ronda-routing-bidi]:

```clojure
(require '[ronda.routing.bidi :as bidi])

(def routes
  (bidi/descriptor
    ["/" {["doc/" :id] :doc}]))
```

This descriptor contains a single route (`:doc`) identified by the path
`/doc/:id` and can be injected into your Ring stack quite easily:

```clojure
(require '[ronda.routing :as routing])

(def app
  (-> (routing/compile-endpoints
        {:doc (constantly {:status 200})})
      (routing/wrap-routing routes)))
```

See [ronda-routing][ronda-routing] for detailed descriptions of the middlewares
and functionality it offers.

[ronda-routing-bidi]: https://github.com/xsc/ronda-routing-bidi

### Step 2: Schemas

Ronda allows for middlewares to be activated per-route by associating them with
a unique middleware key. The schema middleware, as well as the schemas
themselves are expected to be associated with the `:schema` key:

```clojure
(require '[schema.core :as s])

(def routes
  (-> (bidi/descriptor
        ["/" {["doc/" :id] :doc}])
      (routing/enable-middlewares
        :doc {:schema {:get {:params {:id s/Int}}}})))
```

Integration with the stack requires `ronda.routing/meta-middleware`:

```clojure
(require '[ronda.schema :refer [wrap-schema]])

(def app
  (-> (routing/compile-endpoints
        {:doc (constantly {:status 200})})
      (routing/meta-middleware :schema #(wrap-schema % %3))
      (routing/wrap-routing routes)))
```

For more information on the capabilities of ronda's schemas, see [ronda/schema's
README][ronda-schema]. Note that _actually adding_ the schema middleware is not
necessary for ronda-swagger to work - but why wouldn't you?

### Step 3: Swagger

Adding Swagger is a matter of adding the endpoint provided by ronda-swagger, as
well as a path leading to it. The Ring handler can be generated as follows:

```clojure
(require '[ronda.swagger :as swag]
         '[cheshire.core :as json])

(def swagger
  (swag/swagger-handler
    {:info {:title "ronda API", :version "0.1.0"}
     ;; By default, the body is just the Clojure map. This option adds
     ;; JSON encoding.
     :encode #(json/generate-string % {:pretty true})}))
```

Making routes and the stack look as follows:

```clojure
(def routes
  (-> (bidi/descriptor
        ["/" {["doc/" :id]   :doc
              "swagger.json" :swagger}])
      (routing/enable-middlewares
        :doc {:schema {:get {:params {:id s/Int}}}})))

(def app
  (-> (routing/compile-endpoints
        {:doc (constantly {:status 200})})
      (routing/meta-middleware :schema #(wrap-schema % %3))
      (routing/wrap-endpoint :swagger swagger)
      (routing/wrap-routing routes)))
```

Requesting `/swagger.json` will call the `:swagger` handler and generate the
following JSON:

```json
{
  "swagger" : "2.0",
  "info" : {
    "title" : "ronda API",
    "version" : "0.1.0"
  },
  "produces" : [ "application/json" ],
  "consumes" : [ "application/json" ],
  "paths" : {
    "/doc/{id}" : {
      "get" : {
        "parameters" : [ {
          "in" : "path",
          "name" : "id",
          "description" : "",
          "required" : true,
          "type" : "integer",
          "format" : "int64"
        } ],
        "responses" : {
          "default" : {
            "description" : ""
          }
        }
      }
    }
  },
  "definitions" : { }
}
```
