# Senior Java Backend Code Review

## Scope
Reviewed the uploaded repository ZIP, including the assignment brief, Java source, persistence adapters, REST resources, tests, Maven configuration, and CI workflow.

## Summary
The submission has a sensible separation for the warehouse use cases (domain ports, use cases, persistence adapter), implements the required location lookup, store after-commit callback, warehouse lifecycle, and the optional fulfilment limits. The design answers in `QUESTIONS.md` are thoughtful and explain the trade-offs clearly.

**Status: NOT YET CERTIFIED AS SUBMISSION-READY.** The code review found a concrete PATCH correctness defect and important production-hardening gaps. The PATCH defect is corrected in this candidate archive. A full build/test run could not be completed in this environment because Maven Wrapper could not download Maven from `repo.maven.apache.org` (`UnknownHostException`), so no passing test result or coverage percentage is claimed.

## Change made in this review candidate

### Store PATCH semantics
**Original defect:** `StoreResource.patch()` checked the existing entity's fields rather than the incoming request fields. It therefore could overwrite a requested name with `null`, and its `quantityProductsInStock != 0` condition prevented setting stock to zero while also failing to distinguish omitted fields from explicit zero.

**Correction:**
- Changed `quantityProductsInStock` from primitive `int` to nullable `Integer`, allowing PATCH to distinguish omitted from explicitly supplied zero.
- PATCH now updates only non-null fields from the request.
- Empty/null PATCH bodies are rejected with 422.
- Added endpoint tests for stock-only PATCH, name-only PATCH, explicit zero stock, and empty PATCH rejection.

## Findings requiring attention before a production-grade approval

### High: concurrency safety for warehouse capacity and business-unit uniqueness
`CreateWarehouseUseCase` checks the active business-unit code, warehouse count, and aggregate location capacity in application code before inserting. Two concurrent requests can both pass these checks and then both insert, exceeding the location limit or creating duplicate active business-unit codes. `DbWarehouse.businessUnitCode` has no database uniqueness enforcement.

**Recommendation:** enforce active business-unit uniqueness in PostgreSQL (for example, a partial unique index on `businessUnitCode WHERE archivedAt IS NULL`) and serialize capacity-affecting writes per location (row/advisory lock or a dedicated location-capacity row with optimistic/pessimistic locking). Keep application validation for friendly errors, but let database constraints be the final guard.

### High: fulfilment limits are vulnerable to concurrent requests
`FulfilmentService.associate()` counts existing associations and then inserts. Concurrent requests can both observe available capacity and exceed the 2/3/5 limits. The unique constraint only prevents an exact duplicate `(storeId, productId, warehouseId)`; it does not enforce the aggregate limits.

**Recommendation:** serialize association changes for the relevant store and warehouse (consistent lock ordering), or model the limits using transactional counters/locking. Add concurrent integration tests.

### High: legacy notification is after commit but not durable
`StoreResource.afterCommit()` correctly waits for `STATUS_COMMITTED`, so a rolled-back DB transaction does not trigger the legacy gateway. However, if the process stops after commit or the gateway fails, the DB change remains committed and the notification is lost. `LegacyStoreManagerGateway` currently catches all exceptions and prints a stack trace, so callers/monitoring cannot reliably detect delivery failure.

**Recommendation:** for production reliability, write an outbox event in the same DB transaction and dispatch/retry it asynchronously with idempotency. At minimum, replace `printStackTrace()` with structured logging and observable failure handling. The assignment's after-commit requirement is met, but durable delivery is not.

### Medium: error mapping is duplicated and leaks implementation details
`ProductResource` and `StoreResource` each define `ExceptionMapper<Exception>` and return exception class names/messages. This can expose implementation details and creates overlapping global exception handling.

**Recommendation:** centralize exception mapping; return a stable error contract with a correlation/request ID, safe client message, and no internal exception class or stack details. Keep specific domain mappers for warehouse and fulfilment.

### Medium: PATCH and input validation consistency
The Store PATCH issue is fixed in this candidate. Review create/update validation consistently across Store and Product (null request bodies, required names, non-negative stock/price rules as appropriate, and duplicate-name behavior). Avoid relying only on database constraints for client-facing validation.

### Medium: persistence style is inconsistent
Store uses Active Record, Product uses PanacheRepository, Warehouse uses ports/adapters. The `QUESTIONS.md` explanation correctly identifies this. For maintainability, move Store persistence behind a repository when extending the feature; avoid making REST resources responsible for persistence orchestration.

### Low: LocationGateway collection
The static `ArrayList` is never mutated after initialization, but it is still mutable shared state. Prefer `List.of(...)` and an immutable lookup map if lookups become frequent. This is a maintainability improvement, not a current functional blocker.

## Test review
The repository includes unit tests for warehouse use cases and fulfilment rules, Quarkus endpoint tests, repository tests, and a packaged integration test. The Maven configuration sets an 80% JaCoCo line coverage gate and the README documents `./mvnw clean verify`.

**Execution result in this review environment:** `./mvnw clean verify` did not reach Maven build execution. Maven Wrapper failed while trying to download Maven because DNS/network access to `repo.maven.apache.org` was unavailable (`java.net.UnknownHostException`). Therefore:
- Build status: NOT VERIFIED
- Unit/integration test status: NOT RUN
- JaCoCo coverage: NOT MEASURED
- CI status: NOT VERIFIED

Run locally or in GitHub Actions:
```bash
cd java-assignment
./mvnw clean verify
```
Then inspect `java-assignment/target/jacoco-report/index.html` and confirm the GitHub Actions workflow is green.

## Approval recommendation
The architecture and assignment coverage are promising, and the PATCH bug is fixed in this candidate. I would **request changes / hold approval** until:
1. `./mvnw clean verify` passes in an environment with Maven Central access and the 80% gate is met.
2. The concurrent warehouse and fulfilment limit risks are either addressed or explicitly accepted as out of scope for this assignment.
3. The Store legacy integration failure behavior is documented; for production, use an outbox/retry mechanism.

For a time-boxed interview assignment, items 2 and 3 can be presented as known production-hardening considerations if the assignment does not require concurrency guarantees or durable external delivery. Do not claim tests passed until the build is run successfully.
