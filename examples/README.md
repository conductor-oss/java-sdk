# Conductor Java SDK Examples

This module provides comprehensive examples demonstrating how to use the Conductor Java SDK for building distributed applications, workflow orchestration, and AI-native workflows.

## Quick Start

```bash
# Set Conductor server URL
export CONDUCTOR_SERVER_URL="http://localhost:8080/api"

# For Orkes Conductor (optional)
export CONDUCTOR_AUTH_KEY="your-key"
export CONDUCTOR_AUTH_SECRET="your-secret"

# Run all examples end-to-end (default)
./gradlew :examples:run

# Run with specific mode
./gradlew :examples:run --args="--all"      # Run all examples
./gradlew :examples:run --args="--menu"     # Interactive menu
./gradlew :examples:run --args="--validate" # SDK validation only

# Run a specific example
./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.helloworld.Main
```

## Unified Examples Runner

The default main class is [`AgenticExamplesRunner`](src/main/java/io/orkes/conductor/sdk/examples/agentic/AgenticExamplesRunner.java) — a single entry point that runs all AI/LLM examples end-to-end.

| Mode | Command | Description |
|------|---------|-------------|
| **All** | `./gradlew :examples:run` | Run all examples end-to-end |
| **Menu** | `./gradlew :examples:run --args="--menu"` | Interactive selection menu |
| **Validate** | `./gradlew :examples:run --args="--validate"` | SDK validation tests only |

## Examples Overview

### Getting Started

| Example | Description | Run Command |
|---------|-------------|-------------|
| [Hello World](src/main/java/com/netflix/conductor/sdk/examples/helloworld/) | Minimal workflow with worker | `./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.helloworld.Main` |
| [Getting Started](src/main/java/com/netflix/conductor/gettingstarted/) | Step-by-step introduction | `./gradlew :examples:run -PmainClass=com.netflix.conductor.gettingstarted.StartWorkflow` |

### Workflow Patterns

| Example | Description | Run Command |
|---------|-------------|-------------|
| [Workflow Operations](src/main/java/io/orkes/conductor/sdk/examples/workflowops/) | Pause, resume, terminate workflows | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.workflowops.Main` |
| [Shipment Workflow](src/main/java/com/netflix/conductor/sdk/examples/shipment/) | Real-world order processing | `./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.shipment.Main` |
| [Send Email](src/main/java/com/netflix/conductor/sdk/examples/sendemail/) | Email workflow with workers | `./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.sendemail.Main` |
| [Task Domains](src/main/java/com/netflix/conductor/sdk/examples/taskdomains/) | Worker routing with domains | `./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.taskdomains.Main` |
| [Events](src/main/java/com/netflix/conductor/sdk/examples/events/) | Event-driven workflows | `./gradlew :examples:run -PmainClass=com.netflix.conductor.sdk.examples.events.EventHandlerExample` |

### AI & LLM Workflows (Agentic)

All agentic workflows are implemented in [`AgenticExamplesRunner.java`](src/main/java/io/orkes/conductor/sdk/examples/agentic/AgenticExamplesRunner.java).

| Workflow | Description | Run Command |
|----------|-------------|-------------|
| **[AgenticExamplesRunner](src/main/java/io/orkes/conductor/sdk/examples/agentic/AgenticExamplesRunner.java)** | **Run all AI/LLM examples** | `./gradlew :examples:run --args="--all"` |
| `llm_chat_workflow` | Automated chat using `LLM_CHAT_COMPLETE` system task | `./gradlew :examples:run` |
| `multi_turn_conversation` | Multi-turn conversation with history using `LLM_CHAT_COMPLETE` | `./gradlew :examples:run` |
| `multiagent_chat_demo` | Moderator LLM routes turns between two LLM panelists via Switch + DoWhile | `./gradlew :examples:run` |
| `function_calling_workflow` | LLM returns JSON selecting a function; dispatch worker executes it | `./gradlew :examples:run` |
| `llm_chat_human_in_loop` | WAIT task pauses workflow for user input; external task update resumes it | `./gradlew :examples:run` |
| `mcp_ai_agent` | AI agent: discovers MCP tools, plans with LLM, calls MCP tool, summarizes | `./gradlew :examples:run` |
| [RagWorkflowExample.java](src/main/java/io/orkes/conductor/sdk/examples/agentic/RagWorkflowExample.java) | End-to-end RAG: document indexing, semantic search, answer generation | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.RagWorkflowExample` |
| [VectorDbExample.java](src/main/java/io/orkes/conductor/sdk/examples/agentic/VectorDbExample.java) | Vector DB operations: embeddings, indexing, semantic search | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.VectorDbExample` |

### API Management

| Example | Description | Run Command |
|---------|-------------|-------------|
| [Metadata Management](src/main/java/io/orkes/conductor/sdk/examples/MetadataManagement.java) | Task & workflow definitions | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.MetadataManagement` |
| [Workflow Management](src/main/java/io/orkes/conductor/sdk/examples/WorkflowManagement.java) | Start, monitor, control workflows | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.WorkflowManagement` |
| [Authorization Management](src/main/java/io/orkes/conductor/sdk/examples/AuthorizationManagement.java) | Users, groups, permissions | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.AuthorizationManagement` |
| [Scheduler Management](src/main/java/io/orkes/conductor/sdk/examples/SchedulerManagement.java) | Workflow scheduling | `./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.SchedulerManagement` |

## Prerequisites

### Conductor Server

```bash
# Docker (recommended)
docker run -p 8080:8080 conductoross/conductor:latest

# Or using the CLI
npm install -g @conductor-oss/conductor-cli
conductor server start
```

### AI/LLM Examples Prerequisites

For the agentic examples, configure:

1. **LLM API key** — set `OPENAI_API_KEY` or `ANTHROPIC_API_KEY`
2. **LLM integration** — configure an LLM provider in Conductor (name it `openai` or `anthropic`)
3. **MCP server** (for `mcp_ai_agent`) — weather MCP server running on port 3001:

```bash
pip install mcp-weather-server
python3 -m mcp_weather_server --mode streamable-http --host localhost --port 3001 --stateless
```

```bash
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
export OPENAI_API_KEY=your-key        # or ANTHROPIC_API_KEY
export MCP_SERVER_URL=http://localhost:3001/mcp   # optional, defaults to this

./gradlew :examples:run --args="--all"
```

See the [Conductor documentation](https://orkes.io/content/category/integrations) for LLM integration setup.

## Project Structure

```
examples/
├── src/main/java/
│   ├── com/netflix/conductor/
│   │   ├── gettingstarted/          # Getting started examples
│   │   └── sdk/examples/
│   │       ├── helloworld/          # Hello world workflow
│   │       ├── shipment/            # Order processing workflow
│   │       ├── sendemail/           # Email workflow
│   │       ├── taskdomains/         # Task domain routing
│   │       └── events/              # Event-driven workflows
│   └── io/orkes/conductor/sdk/examples/
│       ├── agentic/                 # AI/LLM workflow examples
│       │   ├── AgenticExamplesRunner.java  # Unified runner (all agentic workflows)
│       │   ├── FunctionCallingExample.java # (standalone function calling)
│       │   ├── HumanInLoopChatExample.java # (standalone human-in-loop)
│       │   ├── LlmChatExample.java         # (standalone LLM chat)
│       │   ├── RagWorkflowExample.java     # RAG pipeline
│       │   └── VectorDbExample.java        # Vector DB operations
│       ├── validation/              # SDK validation tests
│       ├── workflowops/             # Workflow operations
│       └── util/                   # Utility classes
└── build.gradle
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CONDUCTOR_SERVER_URL` | Conductor server API URL | `http://localhost:8080/api` |
| `CONDUCTOR_AUTH_KEY` | Authentication key (Orkes) | — |
| `CONDUCTOR_AUTH_SECRET` | Authentication secret (Orkes) | — |
| `OPENAI_API_KEY` | OpenAI API key for LLM examples | — |
| `ANTHROPIC_API_KEY` | Anthropic API key (preferred if set) | — |
| `MCP_SERVER_URL` | MCP server URL for MCP agent example | `http://localhost:3001/mcp` |

### Client Configuration

```java
// Basic configuration
ConductorClient client = ConductorClient.builder()
    .basePath("http://localhost:8080/api")
    .build();

// With authentication (Orkes Conductor)
ConductorClient client = ConductorClient.builder()
    .basePath("https://your-cluster.orkesconductor.io/api")
    .credentials("your-key", "your-secret")
    .build();
```

## Building and Running

```bash
# Build all examples
./gradlew :examples:build

# Run a specific example
./gradlew :examples:run -PmainClass=<fully.qualified.ClassName>

# Run with arguments
./gradlew :examples:run --args="--all"
```

## Learn More

- [Conductor Java SDK Documentation](../README.md)
- [Worker SDK Guide](../java-sdk/worker_sdk.md)
- [Workflow SDK Guide](../java-sdk/workflow_sdk.md)
- [Testing Framework](../java-sdk/testing_framework.md)
- [Official Conductor Documentation](https://orkes.io/content)
