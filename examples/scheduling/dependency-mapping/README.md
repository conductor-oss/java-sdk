# Dependency Mapping

A microservices environment needs a dependency graph. The pipeline discovers services, traces inter-service calls, builds a directed graph, and renders a visualization.

## Workflow

```
dep_discover_services ──> dep_trace_calls ──> dep_build_graph ──> dep_visualize
```

Workflow `dependency_mapping_426` accepts `environment` and `namespace`. Times out after `60` seconds.

## Workers

**DiscoverServicesWorker** (`dep_discover_services`) -- discovers services in the specified environment.

**TraceCallsWorker** (`dep_trace_calls`) -- traces inter-service calls across the discovered service count.

**BuildGraphWorker** (`dep_build_graph`) -- builds the dependency graph from traced calls.

**VisualizeWorker** (`dep_visualize`) -- renders the dependency graph with its graph ID.

## Workflow Output

The workflow produces `serviceCount`, `edgeCount`, `graphId`, `visualizationUrl` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `dependency_mapping_426` defines 4 tasks with input parameters `environment`, `namespace` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify service discovery, call tracing, graph building, and visualization.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
