# Pipeline Versioning in Java Using Conductor : Snapshot Config, Tag, Test, Promote

## Pipeline Configs Change, and You Need to Know What Ran When

Your ETL pipeline's configuration. source tables, transformation rules, destination schemas, changes frequently. A config change on Tuesday breaks the Wednesday morning run, and nobody can tell which configuration was active when. Rolling back means guessing which version of the config file was deployed, or restoring from a backup that may include unrelated changes.

Pipeline versioning means taking a snapshot of the full configuration before each change, tagging it with a version (like `v2.1-staging`), running tests against the tagged version to verify it works, and promoting it to production only if tests pass. Each step produces outputs the next step needs. the snapshot ID feeds into the tag, the tag feeds into the test run, and test results gate the promotion.

## The Solution

**You write the snapshot and promotion logic. Conductor handles version gating, test orchestration, and deployment tracking.**

`PvrSnapshotConfigWorker` captures the current pipeline configuration as a frozen snapshot. `PvrTagVersionWorker` assigns a version tag (e.g., `v2.1`) to the snapshot. `PvrTestPipelineWorker` runs the pipeline against test data using the tagged configuration to verify correctness. `PvrPromoteWorker` deploys the tested, tagged version to the target environment. Conductor records the snapshot, tag, test results, and promotion status for every version. giving you a complete audit trail of what configuration was running in each environment at any point in time.

### What You Write: Workers

Three workers manage the version lifecycle: configuration snapshotting, version tagging, and environment promotion, each gating the next stage to prevent untested configs from reaching production.

| Worker | Task | What It Does |
|---|---|---|
| **PvrPromoteWorker** | `pvr_promote` | Promotes the pipeline version to the target environment (e.g.

### The Workflow

```
pvr_snapshot_config
 │
 ▼
pvr_tag_version
 │
 ▼
pvr_test_pipeline
 │
 ▼
pvr_promote

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
