# Dynamic Data Pipeline in Java Using Conductor : Validate, Transform, Enrich, Publish

## Configurable Pipelines Without Hard-Coded Steps

Different data sources need different processing. an API webhook payload needs schema validation and JSON normalization, a batch CSV upload needs field mapping and type coercion, and a Kafka event needs enrichment from a lookup table before publishing. Hard-coding each pipeline variant means duplicating orchestration logic, and adding a new step (e.g., deduplication, PII masking) means touching every pipeline.

The dynamic workflow approach treats each step as a configurable unit: the validate step has a config saying which fields are required, the transform step knows the target format, the enrich step knows which external API to call, and the publish step knows the destination topic. Each step receives the previous step's output plus its own configuration, making the pipeline a chain of independent, reconfigurable stages.

## The Solution

**You write the validate-transform-enrich logic. Conductor handles stage chaining, retries, and pipeline observability.**

`DwValidateWorker` checks the payload against its validation config (required fields, type constraints). `DwTransformWorker` converts the validated data into the target format specified in its config (e.g., JSON normalization). `DwEnrichWorker` augments the transformed data with external API lookups based on its enrichment config. `DwPublishWorker` sends the final result to the configured event bus target. Each step passes its output to the next, and Conductor ensures the chain runs in order, retries any failed step, and tracks every input/output so you can debug exactly which configuration and data produced each result.

### What You Write: Workers

Four config-driven workers form the data pipeline: validation, transformation, enrichment, and publishing, each parameterized by its own JSON configuration block.

| Worker | Task | What It Does |
|---|---|---|
| **DwEnrichWorker** | `dw_enrich` | Enrich step in a dynamic pipeline. |
| **DwPublishWorker** | `dw_publish` | Publish step in a dynamic pipeline. |
| **DwTransformWorker** | `dw_transform` | Transform step in a dynamic pipeline. |
| **DwValidateWorker** | `dw_validate` | Validate step in a dynamic pipeline.### The Workflow

```
dw_validate
 │
 ▼
dw_transform
 │
 ▼
dw_enrich
 │
 ▼
dw_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
