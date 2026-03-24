# Running a Controlled Chaos Experiment on Payment Service

You suspect your payment service cannot handle 500ms of added latency on half its requests.
Rather than waiting for a real outage, you define an experiment, inject the fault, observe
whether the system stays within SLO, and recover cleanly. This workflow orchestrates the
full experiment lifecycle.

## Workflow

```
service, faultType
       |
       v
+----------------------+     +--------------------+     +--------------+     +--------------+
| ce_define_experiment | --> | ce_inject_failure  | --> | ce_observe   | --> | ce_recover   |
+----------------------+     +--------------------+     +--------------+     +--------------+
  DEFINE_EXPERIMENT-1332      500ms latency on          p99 < 2s, within     fault removed
  success=true                50% of requests           SLO                  system nominal
```

## Workers

**DefineExperimentWorker** -- Takes `service` and `faultType` inputs. Returns a tracking ID
`"DEFINE_EXPERIMENT-1332"` for a latency-injection experiment targeting payment-service.

**InjectFailureWorker** -- Receives the experiment definition. Injects 500ms latency on 50%
of requests. Returns `inject_failure: true`.

**ObserveWorker** -- Monitors the system during the fault. Confirms the system remained within
SLO: `p99 < 2s`. Returns `observe: true`.

**RecoverWorker** -- Removes the injected fault and confirms the system is nominal. Returns
`recover: true`.

## Tests

2 unit tests cover the experiment pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
