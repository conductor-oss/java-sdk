# Querying Three Service Databases in Parallel to Build a Unified View

Each microservice owns its own database: users, orders, and products. Building a customer
dashboard requires querying all three -- but doing it sequentially triples the latency. This
workflow uses FORK_JOIN to query all three databases in parallel, then composes a unified
view from the results.

## Workflow

```
userId
  |
  v
  FORK_JOIN ---------------------------------+
  |                |                         |
  v                v                         v
+------------------+  +--------------------+  +----------------------+
| dps_query_       |  | dps_query_         |  | dps_query_           |
| user_db          |  | order_db           |  | product_db           |
+------------------+  +--------------------+  +----------------------+
  name: "Alice"        orderCount: 12          recentProducts:
  tier: "premium"      lastOrder: "ORD-999"    ["Widget", "Gadget"]
  |                |                         |
  +------ JOIN ---+--------------------------+
               |
               v
      +--------------------+
      | dps_compose_view   |   unified view from 3 databases
      +--------------------+
```

## Workers

**QueryUserDbWorker** -- Queries the user database for `userId`. Returns `name: "Alice"`,
`tier: "premium"`.

**QueryOrderDbWorker** -- Queries the order database. Returns `orderCount: 12`,
`lastOrder: "ORD-999"`.

**QueryProductDbWorker** -- Queries recently viewed products. Returns
`recentProducts: ["Widget", "Gadget"]`.

**ComposeViewWorker** -- Builds a unified view from all three database query results.

## Tests

8 unit tests cover each database query and the view composition.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
