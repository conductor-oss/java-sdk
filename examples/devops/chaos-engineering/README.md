# Chaos Engineering in Java with Conductor : Experiment Design, Fault Injection, Observation, and Recovery

Orchestrates controlled chaos experiments using [Conductor](https://github.com/conductor-oss/conductor). This workflow defines an experiment (target service, fault type like CPU stress, network latency, or pod kill), injects the failure into the live system, observes the system's behavior during the fault (error rates, latency, recovery time), and then recovers by removing the injected failure.

## Proving Your System Breaks Gracefully

Your microservices architecture claims to be resilient, but you've never actually killed a database connection mid-request or added 500ms of network latency to the payment service. Chaos engineering means deliberately injecting failures. Pod kills, CPU stress, network partitions, disk pressure, and observing whether your system degrades gracefully or falls over. Each experiment needs a clear definition (what to break and how), controlled fault injection, real-time observation of the impact, and guaranteed recovery that removes the injected failure regardless of what happened during observation.

Without orchestration, you'd SSH into a server, run a stress-ng command, watch Grafana for a few minutes, then kill the process and hope you remembered to clean up. If the observation step reveals a cascading failure, there's no automated recovery, the fault stays injected while you scramble to fix the secondary problem. There's no record of what fault was injected, how long it ran, or what the error rates looked like during the experiment.

## The Solution

**You write the fault injection and observation logic. Conductor handles experiment sequencing, guaranteed recovery, and complete experiment audit trails.**

Each stage of the chaos experiment is a simple, independent worker. The experiment definer creates a structured experiment plan. Specifying the target service, fault type (CPU stress, network latency, pod kill), blast radius, and success criteria. The fault injector applies the specified failure to the target service in a controlled way. The observer monitors the system during the fault, recording error rates, latency spikes, and whether circuit breakers engaged. The recoverer removes the injected fault, restoring the system to its pre-experiment state. Conductor executes them in strict sequence, ensures recovery always runs even if observation reveals problems, and provides a complete audit trail of every chaos experiment.

### What You Write: Workers

Four workers run the chaos experiment. Defining the fault parameters, injecting the failure, observing system behavior, and recovering to steady state.

| Worker | Task | What It Does |
|---|---|---|
| **DefineExperimentWorker** | `ce_define_experiment` | Defines the chaos experiment parameters: target service, fault type (e.g., latency-injection), and blast radius |
| **InjectFailureWorker** | `ce_inject_failure` | Injects the specified fault (e.g., 500ms latency on 50% of requests) into the target service |
| **ObserveWorker** | `ce_observe` | Monitors system behavior during the experiment and checks SLO compliance (e.g., p99 < 2s) |
| **RecoverWorker** | `ce_recover` | Removes the injected fault and verifies the system has returned to nominal operation |

the workflow and rollback logic stay the same.

### The Workflow

```
ce_define_experiment
 │
 ▼
ce_inject_failure
 │
 ▼
ce_observe
 │
 ▼
ce_recover

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
