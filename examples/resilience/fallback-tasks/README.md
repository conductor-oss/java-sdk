# Fallback Tasks

A data retrieval service depends on a primary API that sometimes goes down. Rather than returning an error, the system must cascade through a secondary API and then a local cache, always returning data to the caller regardless of which source provided it.

## Workflow

```
fb_primary_api ──> SWITCH(apiStatus)
                     ├── "ok" ──> (done, primary data returned)
                     ├── "unavailable" ──> fb_secondary_api
                     └── "error" ──> fb_cache_lookup
```

Workflow `fallback_tasks_demo` accepts `available` as input. The SWITCH task `fallback_switch_ref` evaluates `${primary_api_ref.output.apiStatus}` to route to the appropriate fallback tier.

## Workers

**PrimaryApiWorker** (`fb_primary_api`) -- reads `available` from input (accepts `Boolean` or `String` types). When `true`, returns `apiStatus` = `"ok"`, `data` = `"primary data from main API"`, and `source` = `"primary"`. When `false`, returns only `apiStatus` = `"unavailable"` with no data fields. Always returns `COMPLETED` status -- the SWITCH task inspects `apiStatus` to decide routing.

**SecondaryApiWorker** (`fb_secondary_api`) -- takes no input parameters. Always returns `COMPLETED` with `source` = `"secondary"` and `data` = `"fallback data from secondary API"`.

**CacheLookupWorker** (`fb_cache_lookup`) -- takes no input parameters. Always returns `COMPLETED` with `source` = `"cache"`, `data` = `"stale data from cache"`, and `stale` = `true`.

## Workflow Output

The workflow produces `apiStatus`, `data`, `source` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `fallback_tasks_demo` defines 2 tasks with input parameters `available` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Fallback tasks demo -- primary API with SWITCH-based fallback to secondary API or cache lookup.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

12 tests cover the primary success path, the secondary fallback path, the cache fallback path, and the routing logic for each `apiStatus` value.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
