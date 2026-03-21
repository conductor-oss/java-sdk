# Agent Swarm in Java Using Conductor :  Decompose Research into Parallel Specialist Investigations

Agent Swarm .  decompose a research topic into subtasks, run 4 swarm agents in parallel, then merge findings into a unified report. Uses [Conductor](https://github.

## Research at Scale Needs Parallel Specialization

Researching a topic like "edge computing adoption" thoroughly requires four distinct perspectives: market analysis (market size, growth rates, competitive landscape), technical landscape (architectures, key innovations, standards), real-world use cases (deployments, case studies, ROI data), and future trends (emerging developments, potential disruptions). A single agent doing all four sequentially takes four times as long and tends to produce shallow coverage across all areas.

Swarm research decomposes the topic, assigns each area to a specialist agent, runs all four investigations simultaneously, then merges the results .  capturing cross-references between findings (a market trend explained by a technical innovation, a use case that validates a future prediction). The merge step is critical: it's not concatenation but synthesis, combining findings from different angles into a coherent narrative with citations.

## The Solution

**You write the decomposition, specialist research, and synthesis logic. Conductor handles parallel dispatch, synchronization, and per-agent traceability.**

`DecomposeWorker` breaks the research topic into four subtasks, each with a specific research area and targeted instructions. `FORK_JOIN` dispatches `Swarm1Worker` through `Swarm4Worker` to investigate market analysis, technical landscape, use cases, and future trends simultaneously .  each returning structured findings with citations and confidence scores. After `JOIN` collects all four results, `MergeWorker` synthesizes the findings into a unified report with cross-references between areas, key themes, and a consensus assessment. Conductor runs all four agents in parallel (cutting research time by 4x), retries any agent that fails without re-running the others, and records each agent's findings separately for traceability.

### What You Write: Workers

Six workers run the swarm. Decomposing the topic, dispatching four parallel specialists for market, technical, use-case, and trend research, then merging findings.

| Worker | Task | What It Does |
|---|---|---|
| **DecomposeWorker** | `as_decompose` | Decompose worker .  takes a research topic and breaks it into 4 subtasks, each assigned to a different swarm agent wit.. |
| **MergeWorker** | `as_merge` | Merge worker .  combines results from all 4 swarm agents into a unified research report. Computes totalFindings, avgCo.. |
| **Swarm1Worker** | `as_swarm_1` | Swarm agent 1. Market Analysis specialist. Analyzes market trends, competitive landscape, and adoption rates. Return.. |
| **Swarm2Worker** | `as_swarm_2` | Swarm agent 2. Technical Landscape specialist. Surveys technical approaches, architectures, and key innovations. Ret.. |
| **Swarm3Worker** | `as_swarm_3` | Swarm agent 3. Use Cases specialist. Identifies and evaluates real-world use cases, deployments, and case studies. R.. |
| **Swarm4Worker** | `as_swarm_4` | Swarm agent 4. Future Trends specialist. Forecasts future developments, emerging trends, and potential disruptions. .. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
as_decompose
    │
    ▼
FORK_JOIN
    ├── as_swarm_1
    ├── as_swarm_2
    ├── as_swarm_3
    └── as_swarm_4
    │
    ▼
JOIN (wait for all branches)
as_merge

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/agent-swarm-1.0.0.jar

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
java -jar target/agent-swarm-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agent_swarm_demo \
  --version 1 \
  --input '{"researchTopic": "microservices best practices"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agent_swarm_demo -s COMPLETED -c 5

```

## How to Extend

Each swarm agent owns one research domain. Integrate real sources (SerpAPI for market data, Semantic Scholar for papers, Crunchbase for companies), and the decompose-fork-merge research workflow runs unchanged.

- **Swarm agents** (`as_swarm_1/2/3/4`): integrate with real research APIs: Tavily or SerpAPI for market data, Semantic Scholar or arXiv API for academic papers, Crunchbase for company/funding data, and Google Trends for adoption signals
- **DecomposeWorker** (`as_decompose`): use an LLM to dynamically determine the right research areas based on the topic (a biotech topic might need regulatory analysis instead of market analysis)
- **MergeWorker** (`as_merge`): use an LLM with all four agents' findings as context to generate a synthesized report, or output structured JSON that feeds into a report template engine

Swap in real LLM calls and data sources; the swarm pipeline preserves the same decompose-investigate-merge interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
agent-swarm/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agentswarm/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgentSwarmExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DecomposeWorker.java
│       ├── MergeWorker.java
│       ├── Swarm1Worker.java
│       ├── Swarm2Worker.java
│       ├── Swarm3Worker.java
│       └── Swarm4Worker.java
└── src/test/java/agentswarm/workers/
    ├── DecomposeWorkerTest.java        # 9 tests
    ├── MergeWorkerTest.java        # 10 tests
    ├── Swarm1WorkerTest.java        # 8 tests
    ├── Swarm2WorkerTest.java        # 8 tests
    ├── Swarm3WorkerTest.java        # 8 tests
    └── Swarm4WorkerTest.java        # 8 tests

```
