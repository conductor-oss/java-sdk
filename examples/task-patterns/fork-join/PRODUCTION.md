# Fork-Join — Production Deployment Guide

## Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONDUCTOR_BASE_URL` | Yes | Conductor server API endpoint |

No API keys required. This example uses in-memory data.

## Architecture

The fork-join pattern executes three independent branches in parallel:

```
          +-- GetProductWorker ----+
          |                        |
Input --> +-- GetInventoryWorker --+--> MergeResultsWorker --> Output
          |                        |
          +-- GetReviewsWorker ----+
```

Each branch operates on its own data source with NO shared mutable state.

## Error Handling

- All workers validate `productId` input — missing/blank productId is terminal (`FAILED_WITH_TERMINAL_ERROR`)
- `GetProductWorker` fails terminally for unknown product IDs — no synthetic fallback data
- `MergeResultsWorker` requires ALL three branch outputs — if any branch fails, the merge fails
- In Conductor, a FORK_JOIN with `failOnError=true` will terminate remaining branches if one fails

## Security

- Input validation prevents injection through product IDs
- All data is in-memory; no external service calls
- For production with real APIs: use circuit breakers and rate limiting on each branch

## Deployment

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests
java -jar target/fork-join-1.0.0.jar
```

## Monitoring

- Monitor individual branch task durations to identify slow branches
- The merge step latency equals the slowest branch (parallel execution)
- Alert on `fj_get_product` failures (indicates catalog data issues)
- Alert on `fj_merge_results` failures (indicates broken branch contracts)
