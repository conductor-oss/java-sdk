# Pipeline Pattern in Java Using Conductor : Sequential Data Processing Through Stages

## Data Transformation Requires a Clean Stage-by-Stage Flow

Raw sensor data arrives as unstructured readings. Stage 1 parses the binary payload into structured fields. Stage 2 converts units (Fahrenheit to Celsius, PSI to bar). Stage 3 applies calibration offsets. Stage 4 writes the calibrated data to the time-series database. Each stage must receive the exact output of the previous stage. feeding uncalibrated data to the database stage produces incorrect readings.

Building a pipeline as a monolithic function tangles parsing, conversion, calibration, and storage into a single class. When you need to add a fifth stage (anomaly detection), you're editing a 500-line method instead of adding a standalone worker.

## The Solution

**You write each transformation stage. Conductor handles stage ordering, retries, and data flow between stages.**

`PipStage1Worker` handles initial parsing or ingestion. `PipStage2Worker` applies the first transformation. `PipStage3Worker` performs the next processing step, using the previous stage's output. `PipStage4Worker` completes the pipeline and produces the final result. Each stage is a standalone worker that receives input, transforms it, and passes the result to the next stage. Conductor guarantees strict ordering, retries any failed stage, and records every stage's input and output. so you can inspect the data at any point in the pipeline.

### What You Write: Workers

Four stage workers form a sequential transformation chain: validation, format transformation, metadata enrichment, and final output, each receiving the exact output of the previous stage.### The Workflow

```
pip_stage_1
 │
 ▼
pip_stage_2
 │
 ▼
pip_stage_3
 │
 ▼
pip_stage_4

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
