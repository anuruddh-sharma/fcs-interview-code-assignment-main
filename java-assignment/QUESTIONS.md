# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. The three styles should not stay side by side without a reason.

Store extends PanacheEntity and the resource calls Store.findById, Store.listAll and store.persist directly. That is the Active Record style: the HTTP class is also the persistence client. It is short for a CRUD screen, but it makes the legacy sync harder to test and it hides the transaction boundary inside the resource.

Product keeps the entity and uses an injected PanacheRepository. The resource no longer reaches into static entity methods. That is easier to replace in a test and it is the style I would keep for simple CRUD.

Warehouse goes one step further. The use cases depend on WarehouseStore and LocationResolver, and DbWarehouse is an adapter detail. That split is worth it here because create, replace and archive have rules (location capacity, one active business unit code, stock carried across a replacement). Those rules should not sit in a JAX-RS class or a Panache entity.

If I maintained this code I would:

- Move Store off Active Record and onto a repository, same as Product. The after-commit legacy call is a good example: the resource should orchestrate "save, then notify", and the entity should not be the API for both HTTP and the database.
- Keep the port and the separate domain model for Warehouse. I would not "simplify" it back to PanacheEntity, because replacement reuses a business unit code while the old row stays as history. That is awkward to express if the REST DTO, the domain object and the table row are the same class.
- Stop duplicating the generic ExceptionMapper. ProductResource and StoreResource each register an ExceptionMapper<Exception>. Two mappers for the same type make error handling accidental. One application-wide mapper, plus a specific mapper for domain errors (the warehouse and fulfilment mappers already do this), is enough.
- Add the missing query methods on the port (find by id, active by location) instead of letting the resource run arbitrary Panache queries. The repository is the only place that knows archivedAt is null for an "active" warehouse.

I would not introduce a new framework. Panache is fine. The refactor is about one persistence style per kind of complexity: repository for CRUD, ports for anything with business rules.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first, as used for Warehouse, makes the contract the source of truth. Reviewers can read warehouse-openapi.yaml without starting the app. The paths, status codes and the Warehouse schema are explicit, and a client can be generated from the same file. The cost showed up in this assignment: the generator owns the Java method names (createANewWarehouseUnit) and the return type. Returning 201 is not a normal return value, because the method signature is the bean rather than Response. The spec and the implementation can also drift if someone changes a handler and forgets the yaml.

Code-first, as used for Product and Store, is faster for a small CRUD resource. The validation (422 when the name is missing, 404 when the id is unknown) sits next to the code that enforces it. The cost is that there is no single contract. Product and Store do not appear in the warehouse spec, their error JSON is shaped by a hand-written mapper, and a consumer has to read the resource class to know the payload.

My choice for this system:

- Keep OpenAPI-first for Warehouse. It is the API with replace, archive and several 400 rules. Those rules are part of the contract, not an implementation detail. I would add operationIds so the generated methods have names we choose, and I would keep status codes in the yaml in sync with WarehouseExceptionMapper.
- Keep code-first for Product, Store and the fulfilment association, because they are small and change with the UI. I would still publish them: Quarkus can expose a combined OpenAPI document from annotations, so Swagger UI shows every endpoint even if only Warehouse is generated from yaml.
- I would not generate all three from yaml just for consistency, and I would not drop the yaml for Warehouse just because the generator is awkward. The awkwardness is local (adapter and status code). The contract is what the other teams integrate with.

Fulfilment was added code-first on purpose. The assignment did not specify its paths. A short resource plus tests is easier to review than a second generated API, and it can be promoted to the yaml once the contract is agreed.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would spend the time on the rules that cost money or corrupt history if they are wrong, and I would keep the HTTP tests few and deterministic.

First, unit tests around the use cases, with the store and the location resolver mocked. Create, replace and archive have a lot of branches (unknown location, full location, capacity sum, stock match, already archived) and they do not need PostgreSQL. The fulfilment limits (2 warehouses per product and store, 3 warehouses per store, 5 product types per warehouse) are the same kind of test: a pure function, no database. These fail in milliseconds and they document the rule in the test name.

Second, a smaller set of @QuarkusTest endpoint tests. They prove the pieces work together: the repository filters archived rows, the transaction rolls back when a replacement fails the location check, and the legacy store call runs only after commit. One happy path plus the status codes (201, 400, 404, 409) is enough. I would not duplicate every unit-test branch over HTTP.

Third, one @QuarkusIntegrationTest against the packaged jar, so we know the built application starts and the seed data is visible. That test is slower and it does not need to repeat the whole matrix.

What I would not do first: a browser test of index.html, a native-image run, or a test per line of the legacy temp-file gateway. Those do not protect the warehouse rules.

The original ProductEndpointTest deleted seed row 1 and then asserted it was gone. That couples every later test to class order. New tests create their own products, stores and warehouses and, where a location only allows one warehouse, they archive it before the test ends.

To keep coverage honest over time:

- JaCoCo runs in GitHub Actions on every push and pull request. The build fails if line coverage of our code drops below 80%. Generated OpenAPI classes are excluded so the gate measures the code we maintain.
- The gate is a floor, not the goal. A test that only calls a method to paint a line is not useful. New business rules should arrive with a unit test that names the rule.
- I would watch the shared database. If a test starts depending on MWH.001 still having stock 10, that test is brittle. Seed data is for the list smoke test. Everything else should create what it needs.
- When a bug escapes, I would add the missing case at the lowest layer that can express it (usually the use case), not another end-to-end script.
```
