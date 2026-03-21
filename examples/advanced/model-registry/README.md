# ML Model Registry in Java Using Conductor : Register, Version, Validate, Approve, Deploy

## Models Without a Registry Are Unmanageable

Your team trains dozens of models a month. Without a registry, model artifacts live in S3 buckets with names like `model-final-v2-FIXED.pkl`, nobody knows which version is in production, and deploying a new model means SSH-ing into a server and swapping a file. When the new model degrades accuracy, there's no way to roll back to the previous version because nobody recorded which artifact, metrics, or approval was associated with it.

A model registry formalizes the lifecycle: register the artifact with its training metrics, assign a monotonically increasing version, validate that accuracy/latency meet the deployment threshold, get human or automated approval, and deploy to the serving infrastructure. Each step depends on the previous one. you can't deploy an unapproved model, and you can't approve without validation results.

## The Solution

**You write the registration and validation logic. Conductor handles the approval pipeline, retries, and model lineage tracking.**

`MrgRegisterWorker` stores the model artifact and its training metrics in the registry. `MrgVersionWorker` assigns a version number to the registered model. `MrgValidateWorker` checks the metrics against quality gates. minimum accuracy, maximum latency, no data leakage indicators. `MrgApproveWorker` routes through the approval process (automated or human-in-the-loop). `MrgDeployWorker` pushes the approved, validated model version to the serving infrastructure. Conductor records the full lineage, which artifact, what metrics, which version, who approved, when deployed.

### What You Write: Workers

Five workers manage the model lifecycle: artifact registration, version assignment, quality-gate validation, approval routing, and production deployment, each gating the next stage.### The Workflow

```
mrg_register
 │
 ▼
mrg_version
 │
 ▼
mrg_validate
 │
 ▼
mrg_approve
 │
 ▼
mrg_deploy

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
