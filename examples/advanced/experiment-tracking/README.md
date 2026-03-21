# ML Experiment Tracking in Java Using Conductor : Define, Run, Log Metrics, Compare, Decide

## Experiments Without Reproducibility Are Guesswork

You tuned a hyperparameter, retrained the model, and accuracy went up 2%. Was it the hyperparameter change, or did the training data shift? Without a structured experiment record. hypothesis, configuration, metrics, baseline comparison, you can't answer that question. Teams run dozens of experiments a week, and without systematic tracking, knowledge about what was tried and what worked lives in Slack threads and Jupyter notebooks that nobody can find six months later.

Experiment tracking means defining the experiment upfront (name, hypothesis, baseline metric), running it with recorded configuration, logging the resulting metrics, comparing against the baseline with statistical significance testing, and making a recorded decision to adopt or reject the change. Each step produces data the next step needs. you can't compare without metrics, and you can't decide without the comparison.

## The Solution

**You write the training and comparison logic. Conductor handles experiment sequencing, retries, and reproducibility tracking.**

`ExtDefineExperimentWorker` creates the experiment record with an ID and configuration based on the hypothesis. `ExtRunExperimentWorker` executes the training job and produces metrics (accuracy, loss, latency). `ExtLogMetricsWorker` persists those metrics to the experiment tracking store. `ExtCompareWorker` compares the primary metric against the baseline and determines whether the improvement is statistically significant. `ExtDecideWorker` makes the final adopt/reject decision based on the improvement percentage and significance. Conductor records the full chain. from hypothesis to decision, so every experiment is reproducible and auditable.

### What You Write: Workers

Five workers cover the experiment lifecycle: definition, training execution, metrics logging, baseline comparison, and adoption decision, each capturing one piece of the reproducibility chain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtCompareWorker** | `ext_compare` | Computes improvement percentage vs.### The Workflow

```
ext_define_experiment
 │
 ▼
ext_run_experiment
 │
 ▼
ext_log_metrics
 │
 ▼
ext_compare
 │
 ▼
ext_decide

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
