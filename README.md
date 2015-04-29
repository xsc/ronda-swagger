# ronda-swagger

__ronda-swagger__ brings together:

- [ronda-routing][ronda-routing],
- [ronda-schema][ronda-schema], and
- [ring-swagger][ring-swagger]

to generate [Swagger 2.0][swagger2] specifications for your API.

[![Build Status][travis-badge]][travis]

[ronda-routing]: https://github.com/xsc/ronda-routing
[ronda-schema]: https://github.com/xsc/ronda-schema
[ring-swagger]: https://github.com/metosin/ring-swagger
[swagger2]: https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification
[travis-badge]: https://travis-ci.org/xsc/ronda-swagger.svg?branch=master
[travis]: https://travis-ci.org/xsc/ronda-swagger

## Usage

__Leiningen__ ([via Clojars][clojars])

[![Clojars Project](http://clojars.org/ronda/swagger/latest-version.svg)][clojars]

[clojars]: https://clojars.org/ronda/swagger

Have a look at the [quickstart guide](QUICKSTART.md) for a step-by-step
walkthrough on creating a schema-based, swagger-capable web app.

### Swagger from `RouteDescriptor`

Any [`RouteDescriptor`][route-descriptor] can be immediately transformed into a 
Clojure map representing the Swagger specification using
`ronda.swagger/swagger-json`:

```clojure
(require '[ronda.swagger :as swag])
(swag/swagger-json descriptor)
;; => {:swagger "2.0", :info {:title "Swagger API", :version "0.0.1"}, ...}
```

You can pass an additional map, containing data and options that should be
directly passed to the underlying generator:

```clojure
(swag/swagger-json descriptor {:info {:title "ronda API", :version "v1}})
;; => {:swagger "2.0", :info {:title "ronda API", :version "v1"}, ...}
```

Options passed to [ring-swagger][ring-swagger] are:

- `:ignore-missing-mappings?`
- `:default-response-description-fn`

To have schemas appear in the swagger output, ronda-schema has to be integrated
as described [in its README][ronda-schema-integration].

[route-descriptor]: https://github.com/xsc/ronda-routing#route-descriptors
[ronda-schema-integration]: https://github.com/xsc/ronda-schema#integration-with-rondarouting

### Swagger Ring Handler

ronda-swagger offers a ring-compliant handler to-be-used in combination with
[ronda-routing][ronda-routing]. The `RouteDescriptor` will be read from incoming
requests, producing the Swagger JSON response:

```clojure
(def app
  (-> (swag/swagger-handler {:info {:title "ronda API"}})
      (ronda.routing/wrap-routing descriptor)))
```

Although, you should probably create a separate route pointing at the Swagger
endpoint. Options to `swagger-handler` include those for `swagger-json`, as well
as:

- `:memoize?`: whether to memoize the encoded body,
- `:encode`: a function to use to encode the Swagger map (defaults to
  `cheshire.core/generate-string`).

There is also `swagger-json-response` if you want to get the raw response map
for a descriptor.

### Custom Metadata

Additional swagger-relevant data can be either added to the schemas or the
route metadata. The following two descriptors will result in the same Swagger
output:

```clojure
(def metadata-in-schema
  (routing/enable-middlewares
    descriptor
    :doc {:schema {:get {:params {:id s/Int}
                         :description "Get document."
                         :produces ["text/plain"]
                         ...}}}))
```

And:

```clojure
(def metadata-in-route
  (routing/enable-middlewares
    descriptor
    :doc {:schema {:get {:params {:id s/Int}}}
          :swagger {:description "Get document."
                    :produces ["text/plain]
                    ...}}))
```

Allowed metadata keys are: `:description`, `:summary`, `:tags`, `:consumes`,
`:produces`, `:schemes`, `:external-docs`, `:deprecated?` and `:id`, as well as
every key starting with `:x-`.

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
