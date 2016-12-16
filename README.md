# CUDL Viewer Tagging Service

This repository contains the web service used by [CUDL] to implement its
crowdsourced tagging functionality.

The HTTP API of the web service is described at [docs/api.md].

[CUDL]: https://cudl.lib.cam.ac.uk
[docs/api.md]: docs/api.md

## Building/Running

Maven is used to build the project. To create a runnable jar in `./target/`:

```shell-session
$ mvn package
```

Execute the jar to run the app. See the configuration section below as there are
options which must be specified to start the app.

```shell-session
$ java -jar ./target/cudl-viewer-tagging-0.0.0-SNAPSHOT.jar --spring.config.location=file:///tmp/conf.yaml
```

## Database setup

The database schema used by the app is in
[docs/database-setup.psql](docs/database-setup.psql).

## Configuration

Spring Boot's externalised configuration system is used, allowing config
properties to be set in various ways. An example YAML file is provided at
[docs/config-example.yaml](docs/config-example.yaml).

A common method is to specify a YAML or .properties config file as a command
line argument when starting the app. The `--spring.config.location` argument
accepts a Spring resource path (e.g. `file:` URL) pointing at the config file.

### Database

#### `spring.datasource.url`
A JDBC connection URL to a postgres database, e.g.
`jdbc:postgresql://localhost:5433/cudl-viewer-tagging`.

#### `spring.datasource.username`
The database user's username.

#### `spring.datasource.password`
The database user's password.

### JWT Authentication

JWTs are used for API authentication. They must be signed and their signatures
can be verified using either a shared secret or a public key.

The byte values to use for the shared secret or public key can be specified by
reference to an external resource, such as a file
(`cudl.tagging.jwt.key.location`), or directly as a value in the configuration
(`cudl.tagging.jwt.key.value`).

#### `cudl.tagging.jwt.audience`
The URI which tokens must specify in their audience ([`aud`][jwt-aud]) field.

[jwt-aud]: https://tools.ietf.org/html/rfc7519#section-4.1.3

#### `cudl.tagging.jwt.key.type`
The type of signature used to verify the tokens. Can be either `shared` to use a
shared secret, or `public` to verify the signature against a public key.

#### `cudl.tagging.jwt.key.value`
The value to use for the shared secret or public key. By default the value is
decoded using UTF-8 to obtain bytes.

#### `cudl.tagging.jwt.key.value-encoding`
The method to use to convert the textual value of `cudl.tagging.jwt.key.value`
into bytes. The defaut is `UTF-8`, but any character set supported by the JVM
can be used, or `base64` can be specified.

#### `cudl.tagging.jwt.key.location`
A [Spring resource URL][spring-resources] pointing at the value to use for the
shared secret or public key. For example, to specify a file:
`file:///some/where/keys/mykey.pub`.

[spring-resources]: http://docs.spring.io/spring/docs/current/spring-framework-reference/html/resources.html#resources-implementations

### Tagging Options

The `cudl.tagging.weight.*` options are used when aggregating data sources for
the `/crowdsourcing/tag/{docId}` endpoint.

#### `cudl.tagging.weight.anno`
The numerical value to scale the weights of user-provided annotation values by.

#### `cudl.tagging.weight.removedtag`
The numerical value to scale the weights of user-provided tag removes by. These
are the values created by a user marking a term as inaccurate.

#### `cudl.tagging.weight.tag`
The numerical value used to scale the weights of system-provided tag values. In
CUDL's case, these are produced via textmining literature related to an item.

#### `cudl.imageserver-base-url`
The base URL of an image server used when creating image URLs for RDF exports.

#### `cudl.json-base-url`
The base URL of an web service providing CUDL JSON metadata by classmark.
