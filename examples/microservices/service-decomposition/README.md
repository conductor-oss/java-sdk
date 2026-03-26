# Strangler Fig: Routing Between Monolith and Microservice via Feature Flag

You are extracting a feature from a monolith into a microservice, but you cannot switch
all traffic at once. This workflow checks a feature flag to decide the routing target, then
uses a SWITCH to send the request to either the monolith or the new microservice, and
optionally compares the results to validate equivalence.

## Workflow

```
feature, request
       |
       v
+---------------------------+
| sd_check_feature_flag     |   target: "monolith", percentage: 100
+---------------------------+
       |
       v
  SWITCH on target
  +--"monolith"------------------+--"microservice"--+
  | sd_call_monolith             | sd_call_micro... |
  | source: "monolith"          | source: "micro."|
  | result: "processed"         | result: "proc."|
  +------------------------------+-----------------+
       |
       v
  sd_compare_results (optional)   match: true
```

## Workers

**CheckFeatureFlagWorker** -- Checks `feature` flag. Currently routes to
`target: "monolith"` at `percentage: 100`.

**CallMonolithWorker** -- Processes in the legacy system. Returns `source: "monolith"`,
`result: "processed"`.

**CallMicroserviceWorker** -- Processes in the new service. Returns
`source: "microservice"`, `result: "processed"`.

**CompareResultsWorker** -- Compares monolith and microservice results. Returns
`match: true`.

## Tests

No unit tests for this example.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
