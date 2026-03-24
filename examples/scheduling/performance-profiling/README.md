# Performance Profiling in Java Using Conductor : Instrument, Collect Profiles, Analyze Hotspots, and Recommend

## The Problem

Your service is slow and you need to find out why. Performance profiling requires instrumenting the service to collect profile data (CPU sampling, memory allocation tracking), analyzing the profiles to find hotspots (methods consuming the most CPU, objects causing the most GC), and generating actionable recommendations ("this method is O(n^2). consider a hashmap").

Without orchestration, profiling is a manual process. an engineer SSHs to a server, runs async-profiler, downloads the flame graph, and eyeballs it. There's no automated pipeline from instrumentation to recommendation, and profiles are lost after the session ends.

## The Solution

**You just write the profiler integration and hotspot analysis logic. Conductor handles the instrument-collect-analyze-recommend sequence, retries when profiler attachment fails, and a durable record of every profiling session's findings and recommendations.**

Each profiling step is an independent worker. instrumentation, profile collection, hotspot analysis, and recommendation generation. Conductor runs them in sequence: instrument the target, collect the profile, analyze hotspots, then generate recommendations. Every profiling session is tracked with the exact profile data, findings, and recommendations.

### What You Write: Workers

Four workers run each profiling session: PrfInstrumentWorker attaches the profiler, CollectProfileWorker gathers CPU and memory samples, AnalyzeHotspotsWorker identifies the costliest methods, and a RecommendWorker generates specific optimization suggestions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeHotspotsWorker** | `prf_analyze_hotspots` | Analyzes collected profile samples to identify CPU hotspots (e.g., methods consuming the most CPU) and their percentages |
| **CollectProfileWorker** | `prf_collect_profile` | Collects CPU/memory profile data over the configured duration, returning sample count and profile size |
| **PrfInstrumentWorker** | `prf_instrument` | Attaches a profiler to the target service for the specified profile type (CPU/memory), reporting overhead percentage |
| **RecommendWorker** | `prf_recommend` | Generates optimization recommendations based on hotspot findings, with estimated improvement and a report URL |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
prf_instrument
 │
 ▼
prf_collect_profile
 │
 ▼
prf_analyze_hotspots
 │
 ▼
prf_recommend

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
