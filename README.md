## About the assignment

This repository contains two parts:

- [java-assignment/CODE_ASSIGNMENT.md](java-assignment/CODE_ASSIGNMENT.md) — the implementation tasks
- [case-study/CASE_STUDY.md](case-study/CASE_STUDY.md) — the written scenarios
- [java-assignment/QUESTIONS.md](java-assignment/QUESTIONS.md) — the design questions

The domain overview is in [java-assignment/BRIEFING.md](java-assignment/BRIEFING.md).

## Run the application

The service is a Quarkus application under [java-assignment](java-assignment). You need JDK 17 or newer. `mvnw verify` downloads and starts an embedded PostgreSQL, so Docker is not required for the tests. Live coding (`quarkus:dev`) still uses Quarkus Dev Services and needs Docker, or you can point `application.properties` at your own PostgreSQL.

From `java-assignment`:

```sh
./mvnw quarkus:dev
```

On Windows:

```sh
mvnw.cmd quarkus:dev
```

The demo page is [http://localhost:8080/index.html](http://localhost:8080/index.html).

Warehouse endpoints:

- `GET /warehouse` lists active warehouses
- `POST /warehouse` creates one (`201`)
- `GET /warehouse/{id}` returns a warehouse, including an archived one
- `DELETE /warehouse/{id}` archives it (`204`)
- `POST /warehouse/{businessUnitCode}/replacement` archives the active warehouse and creates the replacement

Fulfilment associations (bonus):

- `POST /fulfilment` with `{ "storeId", "productId", "warehouseId" }`
- `GET /fulfilment` and `GET /fulfilment?storeId={id}`
- `DELETE /fulfilment/{id}`

A product can be fulfilled by at most 2 warehouses per store, a store by at most 3 warehouses, and a warehouse can hold at most 5 product types.

## Tests and coverage

```sh
./mvnw clean verify
```

`clean verify` runs the unit tests, the Quarkus tests, the packaged-jar smoke test, and JaCoCo. The tests start an embedded PostgreSQL, so Docker is not required. The build fails if line coverage of the maintained code drops below 80%. Generated OpenAPI classes are excluded. The HTML report is written to `java-assignment/target/jacoco-report/`.

GitHub Actions runs the same `./mvnw -B clean verify` command on every push and pull request (see `.github/workflows/ci.yml`).

## About the code base

Some of this code is based on https://github.com/quarkusio/quarkus-quickstarts
