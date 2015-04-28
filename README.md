# ronda-swagger

__ronda-swagger__ brings together:

- [ronda-routing][ronda-routing],
- [ronda-schema][ronda-schema], and
- [ring-swagger][ring-swagger]

to generate [Swagger 2.0][swagger2] specifications for your API.

[ronda-routing]: https://github.com/xsc/ronda-routing
[ronda-schema]: https://github.com/xsc/ronda-schema
[ring-swagger]: https://github.com/metosin/ring-swagger
[swagger2]: https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification

## Usage

__Leiningen__ ([via Clojars][clojars])

[![Clojars Project](http://clojars.org/ronda/swagger/latest-version.svg)][clojars]

[clojars]: https://clojars.org/ronda/swagger

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
`/doc/:id`. It can be injected into your Ring stack in the following manner:

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

Integration with the stack requires `ronda.routing/meta-middleware` (there is no
such convenience function in ronda-schema to avoid having it depend on
ronda-routing, by the way):

```clojure
(require '[ronda.schema :refer [wrap-schema]])

(def app
  (-> (routing/compile-endpoints
        {:doc (constantly {:status 200})})
      (routing/meta-middleware :schema #(wrap-schema % %3))
      (routing/wrap-routing routes)))
```

For more information on the capabilities of ronda's schemas, see the
[respective documentation][ronda-schema]. Note that actually adding the schema
middleware is not necessary for ronda-swagger to work - but why wouldn't you?

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

Making routes and the stack look like the following:

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

## License

```
The MIT License (MIT)

Copyright (c) 2015 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
