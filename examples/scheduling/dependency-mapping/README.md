# Dependency Mapping in Java Using Conductor : Service Discovery, Call Tracing, Graph Building, and Visualization

## The Problem

You need to understand how your microservices connect. which services call which, what the critical paths are, and where single points of failure exist. This requires discovering all services, tracing the actual call patterns between them, building a directed graph of dependencies, and rendering it into a visual map that engineers and architects can use.

Without orchestration, dependency mapping is either a manual Confluence diagram that's always outdated or a one-off script that queries service mesh data and dumps JSON. The discovery, tracing, graph construction, and visualization are disconnected steps that someone runs ad hoc when an incident makes them realize they don't know what depends on what.

## The Solution

**You just write the service discovery and call tracing logic. Conductor handles the discover-trace-build sequence, retries when service mesh endpoints are unavailable, and versioned tracking of dependency graph changes over time.**

Each mapping concern is an independent worker. service discovery, call tracing, graph building, and visualization. Conductor runs them in sequence: discover services, trace their interactions, build the graph, then render it. Every mapping run is versioned and tracked, so you can compare dependency changes over time. ### What You Write: Workers

The mapping pipeline chains DiscoverServicesWorker to enumerate active services, TraceCallsWorker to capture inter-service communication, BuildGraphWorker to construct the dependency graph, and a visualization step to render the architecture map.

| Worker | Task | What It Does |
|---|---|---|
| **BuildGraphWorker** | `dep_build_graph` | Constructs a dependency graph from traced call data, counting nodes, edges, and circular dependencies |
| **DiscoverServicesWorker** | `dep_discover_services` | Discovers active services in an environment and returns their names and total count |
| **TraceCallsWorker** | `dep_trace_calls` | Traces inter-service call patterns across discovered services, returning directed edges (caller-to-callee) |
| **VisualizeWorker** | `dep_visualize` | Renders the dependency graph into a visual format and generates a shareable visualization URL |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
dep_discover_services
 │
 ▼
dep_trace_calls
 │
 ▼
dep_build_graph
 │
 ▼
dep_visualize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
