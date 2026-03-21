# Three-Agent Pipeline in Java Using Conductor :  Researcher, Writer, Reviewer

Three-Agent Pipeline. Researcher + Writer + Reviewer with final output assembly. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Quality Content Needs Research, Writing, and Review

A single LLM call to "write an article about quantum computing" produces unreferenced, unreviewed content. The three-agent pipeline mirrors a real editorial process: the researcher finds authoritative sources and extracts key facts, the writer uses those facts to craft a narrative for the target audience, and the reviewer evaluates accuracy, clarity, and completeness .  flagging issues before publication.

Each agent adds value that the others can't. The researcher focuses on finding and validating information without worrying about prose quality. The writer focuses on narrative and engagement without having to verify facts. The reviewer brings a fresh perspective, catching issues that the writer is too close to see. Separating these roles produces higher-quality output than any single agent attempting all three tasks.

## The Solution

**You write the research, writing, and review logic. Conductor handles the editorial pipeline, draft versioning, and quality scoring.**

`ResearcherAgentWorker` investigates the topic and produces structured research with key facts, sources, statistics, and talking points for the target audience. `WriterAgentWorker` crafts the content using the researcher's material, structuring it for the specified audience with appropriate tone and depth. `ReviewerAgentWorker` evaluates the draft against quality criteria .  factual accuracy, clarity, completeness, audience fit, and provides an overall score with specific feedback. `FinalOutputWorker` packages the reviewed content with the review score and any reviewer notes. Conductor chains these four steps and records each agent's output for editorial quality tracking.

### What You Write: Workers

Four workers form the editorial pipeline, the researcher gathers facts, the writer crafts the narrative, the reviewer scores quality, and the final output is assembled.

| Worker | Task | What It Does |
|---|---|---|
| **FinalOutputWorker** | `thr_final_output` | Final output worker .  assembles the complete report from all agent outputs. |
| **ResearcherAgentWorker** | `thr_researcher_agent` | Researcher agent .  gathers key facts, statistics, and sources on a subject. |
| **ReviewerAgentWorker** | `thr_reviewer_agent` | Reviewer agent .  evaluates the draft for accuracy, clarity, and audience fit. |
| **WriterAgentWorker** | `thr_writer_agent` | Writer agent .  produces a draft article incorporating research facts for the target audience. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
thr_researcher_agent
    │
    ▼
thr_writer_agent
    │
    ▼
thr_reviewer_agent
    │
    ▼
thr_final_output

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
java -jar target/three-agent-pipeline-1.0.0.jar

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
java -jar target/three-agent-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow three_agent_pipeline \
  --version 1 \
  --input '{"subject": "microservices best practices", "audience": "sample-audience"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w three_agent_pipeline -s COMPLETED -c 5

```

## How to Extend

Each agent fulfills one editorial role. Integrate web search APIs (Tavily, SerpAPI) for research, an LLM for audience-tailored writing, and a separate LLM as an independent reviewer, and the research-write-review pipeline runs unchanged.

- **ResearcherAgentWorker** (`thr_researcher_agent`): integrate with web search APIs (Tavily, SerpAPI) for current information, academic APIs (Semantic Scholar) for peer-reviewed sources, and fact-checking databases
- **WriterAgentWorker** (`thr_writer_agent`): use GPT-4 or Claude with audience-specific system prompts and the researcher's structured facts as context for high-quality prose generation
- **ReviewerAgentWorker** (`thr_reviewer_agent`): use a different LLM than the writer as the reviewer, with rubric-based scoring (accuracy 1-5, clarity 1-5, completeness 1-5) and specific improvement suggestions

Swap in LLM calls for real content generation; the research-write-review pipeline maintains the same handoff contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
three-agent-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/threeagentpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ThreeAgentPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalOutputWorker.java
│       ├── ResearcherAgentWorker.java
│       ├── ReviewerAgentWorker.java
│       └── WriterAgentWorker.java
└── src/test/java/threeagentpipeline/workers/
    ├── FinalOutputWorkerTest.java        # 8 tests
    ├── ResearcherAgentWorkerTest.java        # 8 tests
    ├── ReviewerAgentWorkerTest.java        # 9 tests
    └── WriterAgentWorkerTest.java        # 8 tests

```
