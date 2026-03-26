# Aggregating User, Order, and Recommendation Data in Parallel

A dashboard page needs the user profile, recent orders, and personalized recommendations --
but fetching them sequentially triples the page load time. This workflow uses FORK_JOIN to
fetch all three in parallel, then merges the results into a single response.

## Workflow

```
userId
  |
  v
  FORK_JOIN ----------------------------------+
  |                |                          |
  v                v                          v
+-----------------+ +------------------+ +------------------------+
| agg_fetch_user  | | agg_fetch_orders | | agg_fetch_             |
|                 | |                  | | recommendations        |
+-----------------+ +------------------+ +------------------------+
  name: "Alice"      recent orders list    items: ["Widget Pro",
  email: alice@co    from user history      "Gadget X"]
  tier: "premium"                           count: 2
  |                |                          |
  +------ JOIN ---+--------------------------+
               |
               v
     +---------------------+
     | agg_merge_results   |   combined from 3 services
     +---------------------+
```

## Workers

**FetchUserWorker** -- Fetches user profile: `name: "Alice"`, `email: "alice@co.com"`,
`tier: "premium"`.

**FetchOrdersWorker** -- Fetches recent orders for the user.

**FetchRecommendationsWorker** -- Generates recommendations: `items: ["Widget Pro",
"Gadget X"]`, `count: 2`.

**MergeResultsWorker** -- Combines all three data sources into a single `merged` response.

## Tests

31 unit tests cover user fetching, order fetching, recommendations, and result merging.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
