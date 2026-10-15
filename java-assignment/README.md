# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of this module:

```sh
./mvnw package
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Tests and coverage

`./mvnw clean verify` starts an embedded PostgreSQL, so Docker is not required to run the tests. JaCoCo fails the build when line coverage drops below 80%. The report is `target/jacoco-report/index.html`. GitHub Actions runs this command from `.github/workflows/ci.yml`.

`./mvnw quarkus:dev` uses Quarkus Dev Services and needs Docker, unless you point the datasource at your own PostgreSQL.

## Fulfilment API

`POST /fulfilment` associates a product with a warehouse for a store:

```json
{ "storeId": 1, "productId": 2, "warehouseId": 3 }
```

`GET /fulfilment` lists associations. `GET /fulfilment?storeId=1` filters by store. `DELETE /fulfilment/{id}` removes one.

Rules: at most 2 warehouses per product per store, at most 3 warehouses per store, and at most 5 product types per warehouse. The warehouse must be active.

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".