# Adaptive concurrency end-to-end tests

End-to-end tests for adaptive worker concurrency against a real OSS Conductor server. Worker containers run with
the same limits as `harness/manifests/deployment.yaml` unless a scenario says otherwise, so the JVM picks its
production defaults (SerialGC and a 123 MB heap at 512Mi).

Conductor runs on the host, started by `server.sh` with the Conductor CLI (`conductor server start`, OSS
the CLI's latest release unless `E2E_SERVER_VERSION` names one, SQLite). Each run starts the server in its own output directory, so it gets an
empty database, and stops it at the end. Workers and the downstream simulator run in Docker and reach the server
through `host.docker.internal`.

Requirements: the Conductor CLI, Docker and JDK 21. Ports 8080 (server), 18090 (simulator) and 9991–9993 (worker
metrics) must be free.

## Functional scenarios

```shell
harness/e2e/run-functional.sh                  # all scenarios, about 30 minutes
harness/e2e/run-functional.sh weights heap     # selected scenarios
```

One worker process polls four task types (`fn_a`..`fn_d`), one `TaskRunnerConfigurer` per type sharing one
controller. The driver loads each scenario's queues before the worker starts polling, samples the server's queue
sizes and the worker's metrics once per second, and checks the outcome against the design's rules.

| Scenario | Setup | Checks |
|---|---|---|
| `weights` | Process target fixed at 16; backlogs 600/300/150/30 of 1 s tasks | Targets match the spec's water-filled shares; executions follow targets; no slot unassigned while demand exceeds the target; throughput while all queues are backlogged splits 9/4/2/1 |
| `rotation` | Process target 2; four queues of 40 tasks | Every queue starts a task in every 15 s window; concurrency never exceeds 2; equal backlogs finish together |
| `starvation` | Process target 4; three backlogs of 2 s tasks plus a domain-routed task every 3 s | Trickle tasks wait at most 5 s and never fall back out of their domain; heavy queues keep progressing |
| `scaling` | 1500 tasks of 2 s I/O on one queue, then 4 minutes idle | Growth steps of at most 20% (min 1, max 16) per control tick up to the ceiling; threads track the target; capacity is used; the target halves at most every 30 s down to one standby slot per queue; idle threads are released |
| `warmstart` | Initial target 32, minimum 16; 1500 tasks, idle, then a 200-task burst | Starts at 32 without a ramp; reaches the ceiling; idle decay stops at 16; idle threads are released; the burst after idle starts at least 16 tasks within 3 s |
| `cpu` | One CPU, 250 ms CPU-bound tasks, starting at target 8 | Reductions of 25% at least 15 s apart, no growth while CPU is saturated, the target settles at 1–2, throughput near one core |
| `heap` | Tasks holding 2–30 MB payloads for 3 s in a 123 MB heap | No `OutOfMemoryError` and every task completes; no poll and no task start while admission is paused |

Every scenario also checks that all workflows completed, no task executed twice, and all slots were released.
Executions are recorded by the workers themselves (`logs/executions-*.csv`), independently of the SDK's metrics.

## Mixed workload

```shell
harness/e2e/run.sh adaptive    # or: fixed, for tuned fixed thread counts
```

Two worker pods, four tasks shared unevenly by four workflows (dynamic fork fan-out, CPU-heavy JSON scoring, a
bounded connection pool, and a domain-routed long-running task with callbacks), a downstream simulator with limited
capacity, and phases for warmup, steady load, a 1000-workflow burst, a 5x downstream slowdown and idle. Phase lengths,
rates and sizes are environment variables read by `E2eDriver`.

## Output

Each run writes `out/<run>/`: `report.json` with every check and observation, `timeseries.csv` with the one-second
samples, the workers' execution logs, and the server and worker logs.
