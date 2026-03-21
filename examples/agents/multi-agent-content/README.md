# Multi-Agent Content Creation in Java Using Conductor :  Research, Write, SEO Optimize, Edit, Publish

Multi-Agent Content Creation .  research, write, optimize SEO, edit, and publish content through a sequential pipeline of specialized agents. Uses [Conductor](https://github.

## Quality Content Requires Specialized Roles

Asking an LLM to "write a blog post about Kubernetes" in a single call produces generic, unresearched content with no SEO optimization and no editorial review. Quality content creation mirrors a real editorial workflow: a researcher gathers facts and sources, a writer crafts the narrative for a specific audience and word count, an SEO specialist adds keywords and meta descriptions and optimizes headings, an editor improves readability and catches errors, and a publisher formats and delivers the final piece.

Each agent builds on the previous one's output .  the writer uses the researcher's sources, the SEO agent works with the writer's draft, and the editor reviews the SEO-optimized version. If the SEO optimization step produces keyword-stuffed prose, the editor catches it. If the editor's changes break SEO titles, the workflow can be extended with a verification step. Without orchestration, this pipeline becomes a single prompt that tries to do everything at once and does nothing well.

## The Solution

**You write the research, writing, SEO, editing, and publishing logic. Conductor handles the editorial pipeline, version tracking, and content delivery.**

`ResearchAgentWorker` gathers source material, key facts, and statistics on the topic for the target audience. `WriterAgentWorker` drafts the article using the research, targeting the specified word count and audience level. `SeoAgentWorker` optimizes the draft for search .  adding keyword density, meta descriptions, alt text, internal linking suggestions, and heading optimization. `EditorAgentWorker` reviews for grammar, style, readability, and factual consistency. `PublishWorker` formats the final content and delivers it to the specified channel. Conductor chains these five agents and records each version of the content (draft, SEO-optimized, edited, published) for editorial tracking.

### What You Write: Workers

Five agents form the editorial pipeline. Researching the topic, writing the draft, optimizing for SEO, editing for quality, and publishing the final piece.

| Worker | Task | What It Does |
|---|---|---|
| **EditorAgentWorker** | `cc_editor_agent` | Editor agent .  polishes the SEO-optimized article, computes metadata, and determines readability grade. Takes article.. |
| **PublishWorker** | `cc_publish` | Publish worker .  publishes the final article with metadata. Takes finalArticle, metadata, and seoScore; returns url.. |
| **ResearchAgentWorker** | `cc_research_agent` | Research agent .  gathers facts and sources on a given topic for the target audience. Returns 4 facts, 3 sources, and .. |
| **SeoAgentWorker** | `cc_seo_agent` | SEO agent .  optimizes an article draft for search engines. Takes draft and topic; returns optimizedArticle, 4 suggest.. |
| **WriterAgentWorker** | `cc_writer_agent` | Writer agent .  composes an article draft from research facts and sources. Takes topic, facts, sources, and wordCount;.. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
cc_research_agent
    │
    ▼
cc_writer_agent
    │
    ▼
cc_seo_agent
    │
    ▼
cc_editor_agent
    │
    ▼
cc_publish

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
java -jar target/multi-agent-content-1.0.0.jar

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
java -jar target/multi-agent-content-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_agent_content_creation \
  --version 1 \
  --input '{"topic": "microservices best practices", "targetAudience": "production", "wordCount": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_agent_content_creation -s COMPLETED -c 5

```

## How to Extend

Each agent owns one editorial role. Connect Tavily for topic research, an LLM for writing, SEMrush for keyword optimization, and WordPress or Ghost for publishing, and the research-write-optimize-edit-publish pipeline runs unchanged.

- **ResearchAgentWorker** (`cc_research_agent`): integrate with Tavily or SerpAPI for web research, Google Trends for topic relevance, and content APIs (Wikipedia, industry databases) for authoritative sources
- **SeoAgentWorker** (`cc_seo_agent`): use Ahrefs or SEMrush APIs for keyword difficulty and volume data, Yoast-style readability scoring, and competitive SERP analysis for the target keywords
- **PublishWorker** (`cc_publish`): push to WordPress via REST API, Ghost CMS, Medium's API, or generate formatted Markdown for static site generators (Hugo, Jekyll)

Replace with real LLM writing and SEO tools; the content pipeline uses the same research-to-publish interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
multi-agent-content/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiagentcontent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiAgentContentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EditorAgentWorker.java
│       ├── PublishWorker.java
│       ├── ResearchAgentWorker.java
│       ├── SeoAgentWorker.java
│       └── WriterAgentWorker.java
└── src/test/java/multiagentcontent/workers/
    ├── EditorAgentWorkerTest.java        # 11 tests
    ├── PublishWorkerTest.java        # 8 tests
    ├── ResearchAgentWorkerTest.java        # 8 tests
    ├── SeoAgentWorkerTest.java        # 8 tests
    └── WriterAgentWorkerTest.java        # 8 tests

```
