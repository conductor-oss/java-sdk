# Workflow Inheritance in Java Using Conductor : Base Workflow with Init, Validate, Process, Finalize

## Different Customer Tiers Need Different Processing, Same Structure

Every customer request follows the same lifecycle: initialize the processing context, validate the input, process the request, and finalize (cleanup, notifications). But premium customers get faster processing with dedicated resources, enterprise customers get custom transformation logic, and standard customers get the default path. Without inheritance, you'd duplicate the entire workflow for each tier, copy-pasting the init/validate/finalize steps and only changing the processing step.

Workflow inheritance lets you define the common structure once (init-validate-process-finalize) and override just the processing step per tier. This is the template method pattern applied to workflows. same skeleton, different specializations.

## The Solution

**You write the tier-specific processing step. Conductor handles the shared lifecycle, retries, and variant tracking.**

`WiInitWorker` initializes the processing context for the request. `WiValidateWorker` validates the input against business rules. `WiProcessStandardWorker` (in this variant) processes the request with standard logic. other workflow variants can swap this step for `WiProcessPremiumWorker` or a custom processor. `WiFinalizeWorker` handles cleanup, notifications, and status updates. Conductor runs the same init-validate-finalize structure for every tier, and each execution records which processing variant was used.

### What You Write: Workers

Five workers implement the template method pattern: shared init, validation, and finalization steps plus two tier-specific processors (standard and premium), specializing only the processing stage per customer tier.

### The Workflow

```
wi_init
 │
 ▼
wi_validate
 │
 ▼
wi_process_standard
 │
 ▼
wi_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
