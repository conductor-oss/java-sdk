# Event Merge in Java Using Conductor

Event merge workflow that collects events from three parallel streams via FORK_JOIN, merges the results, and processes the merged output. ## The Problem

You need to collect events from multiple independent streams and merge them into a single unified dataset. Events from Stream A, Stream B, and Stream C must be collected in parallel (since they are independent), merged into a combined result set preserving source attribution, and then processed as a whole. Sequential collection would multiply latency by the number of streams.

Without orchestration, you'd spawn threads to poll each stream, synchronize with barriers or futures, manually merge results while handling partial failures (one stream is unavailable while others succeed), and deduplicate events that appear in multiple streams.

## The Solution

**You just write the per-stream collection, merge, and processing workers. Conductor handles parallel stream collection, per-stream timeout isolation, and automatic join before merging.**

Each stream consumer is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting from all three streams in parallel via FORK_JOIN, merging the results after all complete, processing the merged output, retrying any failed stream independently, and tracking every merge operation. ### What You Write: Workers

Five workers merge multi-source data: CollectStreamAWorker, CollectStreamBWorker, and CollectStreamCWorker each poll their independent source in parallel via FORK_JOIN, MergeStreamsWorker combines the results, and ProcessMergedWorker handles the unified dataset.

| Worker | Task | What It Does |
|---|---|---|
| **CollectStreamAWorker** | `mg_collect_stream_a` | Collects events from stream A (API source). |
| **CollectStreamBWorker** | `mg_collect_stream_b` | Collects events from stream B (mobile source). |
| **CollectStreamCWorker** | `mg_collect_stream_c` | Collects events from stream C (IoT source). |
| **MergeStreamsWorker** | `mg_merge_streams` | Merges events from three streams into a single list. |
| **ProcessMergedWorker** | `mg_process_merged` | Processes the merged event list. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
FORK_JOIN
 ├── mg_collect_stream_a
 ├── mg_collect_stream_b
 └── mg_collect_stream_c
 │
 ▼
JOIN (wait for all branches)
mg_merge_streams
 │
 ▼
mg_process_merged

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
