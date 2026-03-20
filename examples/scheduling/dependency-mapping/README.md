# Dependency Mapping in Java Using Conductor :  Service Discovery, Call Tracing, Graph Building, and Visualization

A Java Conductor workflow example for mapping service dependencies .  discovering services in an environment, tracing inter-service calls, building a dependency graph, and visualizing the architecture.

## The Problem

You need to understand how your microservices connect .  which services call which, what the critical paths are, and where single points of failure exist. This requires discovering all services, tracing the actual call patterns between them, building a directed graph of dependencies, and rendering it into a visual map that engineers and architects can use.

Without orchestration, dependency mapping is either a manual Confluence diagram that's always outdated or a one-off script that queries service mesh data and dumps JSON. The discovery, tracing, graph construction, and visualization are disconnected steps that someone runs ad hoc when an incident makes them realize they don't know what depends on what.

## The Solution

**You just write the service discovery and call tracing logic. Conductor handles the discover-trace-build sequence, retries when service mesh endpoints are unavailable, and versioned tracking of dependency graph changes over time.**

Each mapping concern is an independent worker .  service discovery, call tracing, graph building, and visualization. Conductor runs them in sequence: discover services, trace their interactions, build the graph, then render it. Every mapping run is versioned and tracked, so you can compare dependency changes over time. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

The mapping pipeline chains DiscoverServicesWorker to enumerate active services, TraceCallsWorker to capture inter-service communication, BuildGraphWorker to construct the dependency graph, and a visualization step to render the architecture map.

| Worker | Task | What It Does |
|---|---|---|
| **BuildGraphWorker** | `dep_build_graph` | Constructs a dependency graph from traced call data, counting nodes, edges, and circular dependencies |
| **DiscoverServicesWorker** | `dep_discover_services` | Discovers active services in an environment and returns their names and total count |
| **TraceCallsWorker** | `dep_trace_calls` | Traces inter-service call patterns across discovered services, returning directed edges (caller-to-callee) |
| **VisualizeWorker** | `dep_visualize` | Renders the dependency graph into a visual format and generates a shareable visualization URL |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 426: Dependency Mapping ===

Step 1: Registering task definitions...
  Registered: dep_discover_services, dep_trace_calls, dep_build_graph, dep_visualize

Step 2: Registering workflow 'dependency_mapping_426'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [graph] Building dependency graph
  [discover] Discovering services in
  [trace] Tracing inter-service calls across
  [visualize] Rendering dependency graph

  Status: COMPLETED
  Output: {graphId=..., nodes=..., edgeCount=..., circularDeps=...}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/dependency-mapping-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/dependency-mapping-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dependency_mapping_426 \
  --version 1 \
  --input '{"environment": "sample-environment", "production": "sample-production", "namespace": "sample-name"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dependency_mapping_426 -s COMPLETED -c 5
```

## How to Extend

Each worker tackles one mapping phase .  connect the service discovery worker to your service mesh (Istio, Linkerd) or Kubernetes API, the visualizer to generate real architecture diagrams, and the discover-trace-graph-visualize workflow stays the same.

- **BuildGraphWorker** (`dep_build_graph`): construct a directed graph using JGraphT or Neo4j, computing metrics like PageRank and betweenness centrality
- **DiscoverServicesWorker** (`dep_discover_services`): query Kubernetes service registry, Consul, AWS ECS/EKS, or your CMDB for active services
- **TraceCallsWorker** (`dep_trace_calls`): analyze service mesh data (Istio, Linkerd), distributed traces (Jaeger, Zipkin), or API gateway logs for call patterns

Connect to your service mesh or Kubernetes API, and the dependency mapping orchestration operates without modification.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
dependency-mapping/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dependencymapping/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DependencyMappingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BuildGraphWorker.java
│       ├── DiscoverServicesWorker.java
│       ├── TraceCallsWorker.java
│       └── VisualizeWorker.java
└── src/test/java/dependencymapping/workers/
    └── DiscoverServicesWorkerTest.java        # 2 tests
```
