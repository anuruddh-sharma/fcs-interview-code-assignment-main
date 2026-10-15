# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

Cost allocation fails when the thing we charge is not the thing that consumed the cost. In this domain a Warehouse is not only a business unit code. Replacement archives the old row and creates a new one with the same code, so a cost posted to "MWH.001" is ambiguous: it might belong to the building we just left, or to the one that took over. Labor, rent and a transport lane are often tied to a physical site and a period. Inventory holding cost is tied to stock. Overhead is shared. If all of those land on the business unit code alone, the archived warehouse loses its history and the new warehouse inherits costs it never incurred.

The practical split I would use:

- Direct costs stay on the warehouse generation (the row, with its id and its createdAt/archivedAt) or on the store, for a date range. Rent, site labor and depreciation follow the generation. Last-mile transport can follow the store or the lane between a warehouse and a store.
- Shared costs need an explicit driver. Typical drivers in fulfilment are units shipped, cube, kilogram, order lines and labor hours. The driver has to be one the operation already records, otherwise finance invents a percentage and nobody can explain a variance.
- Location capacity is a constraint, not a cost. A location such as AMSTERDAM-001 has a maximum number of warehouses and a maximum summed capacity. An allocation model that ignores that will "optimize" a warehouse the location cannot host.

Before drawing the boundary of a cost-control tool I would ask:

- Who owns a cost when two warehouses serve one store? The fulfilment rule already allows a product to be fulfilled by two warehouses per store, and a store by three warehouses. Split rules have to match that, or the P&L will not match the network.
- Is the allocation for statutory reporting, for operational decisions, or both? Statutory needs a stable chart of accounts and an audit trail. Operational needs a weekly view by warehouse and store. One model rarely serves both without a version of the rules.
- What is the grain: shipment, day, or month? Finer grain explains more and costs more to store and reconcile.
- How are returns, shrinkage and inter-warehouse transfers treated? They move cost without a customer sale.

In similar allocation work, the expensive mistake was a many-to-many (warehouse, store, product) with no effective dates. After a warehouse replacement, reports mixed two cost structures under one code. I would key facts by warehouse id plus period, and treat the business unit code as the label operators still use.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

I would not start with a list of savings ideas. I would start from the cost view in scenario 1 and from the service levels the company will not break: in-stock, delivery promise, and the number of warehouses a store is willing to receive from. The model already caps that (at most three warehouses per store, two per product per store, five product types per warehouse). Any optimization that needs a fourth warehouse for a store is not a small configuration change. It is a rule change, and it should be explicit.

Strategies that usually pay off in this kind of network, and what I would expect:

- Inventory positioning. Move fast products closer to the stores that sell them, and stop duplicating slow products across warehouses that are already at the five-product cap. Expected outcome: less transport and less leftover stock, with a risk of lower availability if the forecast is wrong. Pilot on one location, compare stock-out rate before widening it.
- Filling existing capacity before opening a warehouse. Location max capacity and max warehouse count are already in the domain. A new warehouse at a full location is rejected for a reason. Expected outcome: avoid rent and labor for a site the location cannot really support. The check is the create validation, not a spreadsheet after the fact.
- Labor and transport, in that order of controllability. Shift patterns and pick paths move faster than a new building. Mode shift (parcel versus line-haul) is next. A warehouse replacement (scenario 5) is the expensive lever and should be last, justified by a gap the cheaper levers cannot close.
- Store fulfilment mix. If one of three warehouses supplies a handful of units, dropping it cuts receiving cost at the store. Expected outcome: fewer inbound deliveries. The risk is a longer lead time on the products that warehouse was the only source for.

How I would prioritize: size the annual cost, the service risk, and the time to reverse the decision. Rank by cost saved per unit of service risk, not by the largest absolute number. Implement as a pilot on one store cluster, with a baseline period, a target (for example transport cost per unit, and fill rate), and a stop rule. Only then change the network data: which warehouses fulfil which products for which stores.

Questions I would take into the scoping workshop: which service metric is protected, who can approve a lower service level for a lower cost, and whether "savings" are cash (closed site, fewer trailers) or an allocation change that does not leave the company.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

The cost tool is not the ledger. If it becomes a second place where money is "true", finance and operations will disagree and both will be wrong in a different way. The benefit of integration is that operational events (a warehouse created, replaced, or archived, a store's stock changed, a fulfilment link added) become postings and dimensions the finance system already understands, soon enough that a weekly trading review is not a month-end surprise.

What the company gets: one chart of accounts, cost centers that follow the warehouse generation rather than only the business unit code, fewer manual journals, and a reconciliation the auditor can follow. Operations gets a cost view that matches the network they actually run.

I would not promise that every number is synchronous. "Real time" here should mean the operational system commits first and the financial system receives a confirmed fact, with a lag of seconds or minutes, plus a daily reconciliation for anything that failed. That is the same lesson as the store legacy sync in this code base: the downstream call runs after the database commit, and a rollback must not notify the other system. A cost event should use the same idea, usually an outbox written in the same transaction as the warehouse or store change, then a publisher that retries. The legacy temp-file gateway is a stand-in; a real publisher needs idempotency, because finance must not post the replacement twice.

Seamless, in practice:

- Map once. Warehouse id, business unit code, store id and location become dimensions. The map is versioned. Replacing a warehouse adds a dimension value; it does not overwrite the old one.
- Contract the message (warehouse archived, warehouse opened, cost bucket, amount, currency, period). Reject unknown codes instead of parking them in a suspense account no one reads.
- Reconcile. Count and sum of messages versus journals, every day, with an owner. A silent retry loop is not a control.
- Decide the system of record per attribute. Quantities and network structure live here. Money and the close live in finance. The cost tool reads both and does not author a journal on its own unless finance has agreed the posting rule.

Questions: which system closes the period, how late an event may arrive and still hit the current period, and whether a failed sync blocks the warehouse operation or only the report. I would block the report and alert, not the goods movement, unless the missing posting is a compliance issue.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

A fulfilment budget is a plan for capacity and service, expressed as money. Without it, a warehouse replacement or a new store fulfilment link is approved because the operation is busy, not because the cost fits a year. Forecasting is what tells you the budget is stale before the peak season, not after.

I would design the system around drivers, not around a single cost number copied forward:

- Volume: orders, units, lines, by store and by product. Seasonality matters more than a flat run-rate. The current stock on a warehouse is a snapshot, not a forecast.
- Network: which warehouses fulfil which stores, within the limits already enforced (two warehouses per product per store, three per store, five product types per warehouse). A forecast that assumes a fourth warehouse for a store will not match what the application allows.
- Capacity: location max capacity and max number of warehouses. A volume forecast that needs more capacity than the location allows is a signal to move volume, replace a warehouse, or change the location limit. It should not be silently clipped.
- Rates: labor per unit, transport per drop, rent per site, inventory holding percent. Rates and volumes are stored separately so a variance can say "we shipped more" versus "the rate got worse".
- Events: a replacement has a project budget (move, dual running, opening stock) and a new run-rate. Those are different forecasts. Putting the move cost into the weekly operating forecast hides both.

The budget needs versions (submitted, approved, forecast 1, forecast 2) and a lock for the closed period. Actuals from scenario 3 land on the same dimensions, so variance is warehouse generation plus store plus cost type plus period. I would keep a short horizon in weeks for labor and transport, and a year for rent and replacement projects.

Accuracy comes from measuring the forecast, not from a more complicated model on day one. I would track bias and error on units and on cost, by location, and review the drivers that moved. Questions for the business: planning calendar, who approves a reforecast, which service level the volume forecast must protect, and how much dual-running cost is allowed when a warehouse is replaced.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

Replacement is already a domain operation: the active warehouse for a business unit code is archived, and a new row is created with the same code. Stock must match, and the new capacity must be able to hold that stock. Cost control has to follow that shape.

Preserving history matters because the archived warehouse still explains the past. Rent, labor, write-off and the project cost of leaving the site belong to that generation. If those amounts are moved onto the new warehouse, two things break. The old site can never be judged (was the replacement worth it?), and the new site starts over budget for costs it did not cause. The business unit code is the name operators keep using. It is not the cost-center key. The warehouse id, with createdAt and archivedAt, is the key. Reports for "how is MWH.001 doing now" filter to the active row. Reports for "what did this area cost last year" include the archived rows for that code.

The new warehouse should be budgeted on its own capacity, location and expected throughput, plus a separate project budget for the move: dual running while both sites are open, transport of the opening stock, and any exit cost on the old site. The stock-match rule is useful here. The opening stock is not a new purchase; it is a transfer. The inventory value moves. The handling cost of the move does not disappear, and it should hit the project, not the new warehouse's weekly run-rate.

Keeping the new operation inside budget then means:

- A run-rate budget that starts when the new warehouse becomes the active one, compared with actuals posted only to that id.
- A project budget that is allowed to finish, with its own variance, and is not used to excuse a high run-rate six months later.
- A check that the new capacity and the location limits still hold. The application already rejects a replacement that the location cannot host, and it rolls the archive back if the create fails. The cost view should use that same outcome: no archive, no "new warehouse" spend.
- Fulfilment links are not moved automatically. The old warehouse can keep its historical product and store links; the new one is associated again, up to five product types. That is the moment to drop links that were only there because of the old building, which is often where the ongoing cost actually changes.

Questions I would ask before the replacement is approved: the last full year of cost for the archived generation, the dual-running window, which costs transfer with the stock, and the run-rate the new capacity is expected to beat. If those numbers are not available, the replacement is a building project without a cost-control case, and the history we fail to keep is the history we will wish we had at the next review.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
