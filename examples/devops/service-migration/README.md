# Migrating Payment Service to a New Environment

Moving a service between environments without assessing its dependencies first risks
breaking downstream consumers. This workflow assesses payment-service (3 dependencies, 2
data stores), replicates it to the target environment, cuts over traffic, and validates all
endpoints respond correctly.

## Workflow

```
service, sourceEnv, targetEnv
            |
            v
+--------------+     +------------------+     +----------------+     +----------------+
| sm_assess    | --> | sm_replicate     | --> | sm_cutover     | --> | sm_validate    |
+--------------+     +------------------+     +----------------+     +----------------+
  ASSESS-1338         service replicated      traffic switched        all endpoints
  3 dependencies,     to target               to new environment      responding
  2 data stores
```

## Workers

**AssessWorker** -- Assesses payment-service: finds 3 dependencies and 2 data stores.
Returns `assessId: "ASSESS-1338"`.

**ReplicateWorker** -- Replicates the service to the target environment. Returns
`replicate: true`.

**CutoverWorker** -- Switches traffic to the new environment. Returns `cutover: true`.

**ValidateWorker** -- Confirms all endpoints are responding correctly. Returns
`validate: true`.

## Tests

2 unit tests cover the service migration pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
