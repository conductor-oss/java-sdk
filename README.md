# Java SDK for Conductor

[![CI](https://github.com/conductor-oss/conductor-java-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/conductor-oss/conductor-java-sdk/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.conductoross/conductor-client.svg)](https://search.maven.org/search?q=g:org.conductoross)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)](https://www.oracle.com/java/technologies/downloads/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

The Java SDK for [Conductor](https://www.conductor-oss.org/) lets you build durable AI agents and workflow workers. Conductor coordinates retries, state, and observability while your Java code runs wherever you deploy it.

**Get involved:** [⭐ Conductor OSS](https://github.com/conductor-oss/conductor) · [Choose a Conductor OSS contribution](https://github.com/conductor-oss/conductor/contribute) · [Contribution guide](https://github.com/conductor-oss/conductor/blob/main/CONTRIBUTING.md)

**Using an AI coding agent?** Load [Conductor Skills](https://github.com/conductor-oss/conductor-skills) so it can create, run, and operate Conductor workflows:

```shell
npm install -g @conductor-oss/conductor-skills && conductor-skills --all
```

## Choose your path

| I want to… | Start here |
|---|---|
| Build a durable AI agent with tools and human approval | [Run an AI agent example](#ai-agent-quickstart) |
| Bring an existing Google ADK or LangChain4j agent | [Use framework bridges](#google-adk-and-langchain4j) |
| Build a durable workflow and Java worker | [Run the core hello-world example](#workflow-and-worker-quickstart) |
| Browse all examples | [AI agent guide](docs/agents/README.md) · [Core examples](examples/README.md) |
| Navigate the SDK documentation | [Documentation hub](docs/README.md) |

## Why Conductor?

- **Survive process failures:** execution state is durable, so agents and workflows resume from completed work.
- **Build dynamic agent graphs:** define workflow graphs in Java or let an LLM plan them at runtime. Conductor executes plans as durable sub-workflows, so agents can plan, execute, observe, and replan complex work instead of relying on a transient in-process loop.
- **Run tools as distributed tasks:** scale Java workers independently while Conductor manages retries and delivery.
- **Orchestrate long-running work:** combine AI, schedules, events, and human approval without holding application threads open.
- **See every execution:** inspect inputs, outputs, tool calls, retries, and status through one execution model.

**See the real graph:** [`Example115PlannerContext`](agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example115PlannerContext.java) has an LLM turn onboarding policy into a KYC → account → email → conditional kickoff graph. Conductor compiles the plan into a durable sub-workflow, pipes outputs between steps, executes it, and the example reads back the generated sub-workflow to print the actual task trace.

```shell
./gradlew :agent-examples:run \
  -PmainClass=org.conductoross.conductor.ai.examples.Example115PlannerContext
```

Prefer to construct the graph in code? [`Example108PlanExecuteRefs`](agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example108PlanExecuteRefs.java) builds a typed `Plan` with dependencies and cross-step output references, then runs it through the same durable sub-workflow execution path.

## Requirements and compatibility

- Java 21+
- A running OSS/Orkes Conductor server. For local development, use the [Conductor CLI](docs/server-setup.md) (`npm install -g @conductor-oss/conductor-cli`; then `conductor server start`). Docker remains available for containerized examples.
- Maven 3.8+ when running standalone core examples without their launcher script; Gradle is included for this repository's agent examples.

The CI workflows are the source of truth for the server versions exercised by this SDK. See the [agent E2E matrix](.github/workflows/agent-e2e.yml) for its pinned server version.

## Install the SDK

Replace `<VERSION>` with a published version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).

### AI agents

```gradle
dependencies {
    implementation 'org.conductoross:conductor-client-ai:<VERSION>'
}
```

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client-ai</artifactId>
    <version>&lt;VERSION&gt;</version>
</dependency>
```

Google ADK and LangChain4j are optional dependencies; use the versions and setup in the [Google ADK guide](docs/agents/frameworks/google-adk.md) or [LangChain4j guide](docs/agents/frameworks/langchain4j.md).

### Workflows and workers

```gradle
dependencies {
    implementation 'org.conductoross:conductor-client:<VERSION>'
}
```

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>&lt;VERSION&gt;</version>
</dependency>
```

### Modules

| Module | Use it for |
|---|---|
| `conductor-client-ai` | Durable AI agents, tools, guardrails, handoffs, and framework bridges |
| `conductor-client-ai-spring` | Spring auto-configuration for AI agents |
| `conductor-client` | Workflow, task, worker, metadata, and scheduler clients |
| `conductor-client-spring` | Spring auto-configuration for the core client |
| `conductor-client-spring-boot4` | Spring Boot 4 auto-configuration for the core client |
| `conductor-client-metrics` | Prometheus metrics collection |

## AI agent quickstart

Use this path when your agent needs LLM reasoning, tools, guardrails, handoffs, or human approval. Configure the LLM provider credential on the **Conductor server process** first—setting it only in the client shell is not enough. The [agent getting-started guide](docs/agents/getting-started.md) covers local and remote server setup.

```shell
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
export CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini

# Run the maintained basic-agent example from this repository.
./gradlew :agent-examples:run \
  -PmainClass=org.conductoross.conductor.ai.examples.Example01BasicAgent
```

Expected outcome: the example prints an `AgentResult` containing the model response. See the [AI agent guide](docs/agents/README.md), [tools guide](docs/agents/concepts/tools.md), and [agent examples](agent-examples/src/main/java/org/conductoross/conductor/ai/examples/) for the next step.

### Google ADK and LangChain4j

Keep using the Java agent framework your team already knows. The SDK bridges native [Google ADK](docs/agents/frameworks/google-adk.md) `BaseAgent` and `LlmAgent` instances into durable Conductor agents, including tools and sub-agent graphs. It also turns existing [LangChain4j](docs/agents/frameworks/langchain4j.md) `@Tool`-annotated POJOs into Conductor worker tools and supports LangChain4j `ChatModel`-based agents.

Start with the [Google ADK examples](agent-examples/src/main/java/org/conductoross/conductor/ai/examples/adk/) or the focused [LangChain4j bridge guide](docs/agents/frameworks/langchain4j.md).

## Workflow and worker quickstart

This maintained example registers a workflow, starts a Java worker, executes the workflow, and prints `Result: PASSED`.

### Recommended: use a CLI-managed server

```shell
# Start the local server once (see docs/server-setup.md).
conductor server start
export CONDUCTOR_SERVER_URL=http://localhost:8080/api

# The launcher reuses the explicitly configured server.
cd examples/basics/hello-world
./run.sh
```

Expected outcome:

```text
Status: COMPLETED
Output: {greeting=Hello, Developer! Welcome to Conductor.}
Result: PASSED
```

Open the CLI-managed server UI at [http://localhost:8080](http://localhost:8080), then stop it when finished:

```shell
conductor server stop
```

### Optional: let the launcher manage Docker

Leave `CONDUCTOR_SERVER_URL` unset and run the same launcher. It starts the example's Docker Compose server; its UI is [http://localhost:1234](http://localhost:1234). Stop that path with `docker compose down` from `examples/basics/hello-world`.

For any existing server, set `CONDUCTOR_SERVER_URL` explicitly before running the launcher. For worker patterns, workflow definitions, and testing, continue with the [core examples catalog](examples/README.md), [worker guide](docs/workers.md), and [workflow guide](docs/workflows.md).

## Common tasks

| Need | Start with |
|---|---|
| Build Java AI agents | [Agent concepts](docs/agents/concepts/agents.md) |
| Add tools and human approval | [Agent tools](docs/agents/concepts/tools.md) |
| Use another agent framework | [Google ADK](docs/agents/frameworks/google-adk.md) · [LangChain4j](docs/agents/frameworks/langchain4j.md) · [LangGraph4j](docs/agents/frameworks/langgraph4j.md) |
| Deploy, serve, and run agents | [Agent runtime modes](docs/agents/concepts/deploy-serve-run.md) |
| Implement and scale Java workers | [Workers guide](docs/workers.md) · [reliability](docs/reliability.md) |
| Define and evolve workflows | [Workflows guide](docs/workflows.md) · [lifecycle/versioning](docs/workflow-lifecycle.md) |
| Upload/download workflow-scoped files | [FileClient guide](docs/file-client.md) |
| Test workflows and workers | [Workflow test harness](docs/workflow-testing.md) |
| Expose worker metrics | [Client metrics](conductor-client-metrics/README.md) |
| Configure Spring applications | [Boot 3](conductor-client-spring/README.md) · [Boot 4](conductor-client-spring-boot4/README.md) · [AI Spring guide](docs/agents/spring-boot.md) |
| Manage schedules and events | [Schedules/events guide](docs/schedules-events.md) |
| Find typed clients and Javadocs | [Core API map](docs/api-map.md) |

## Troubleshooting

| Symptom | Check |
|---|---|
| Connection refused | The server is healthy at `http://localhost:8080/health`; `CONDUCTOR_SERVER_URL` ends in `/api`. |
| Task remains `SCHEDULED` | A worker is polling the exact task type and has enough threads. |
| Authentication failure | `CONDUCTOR_AUTH_KEY` and `CONDUCTOR_AUTH_SECRET` are set for the target server. |
| AI agent cannot call a model | The server—not only the client process—has a configured LLM provider and model. |

## Support and project policies

**Contribute upstream:** [Choose a Conductor OSS contribution](https://github.com/conductor-oss/conductor/contribute) · [Read the Conductor OSS contribution guide](https://github.com/conductor-oss/conductor/blob/main/CONTRIBUTING.md)

- [SDK issues](https://github.com/conductor-oss/conductor-java-sdk/issues) for Java SDK bugs and feature requests
- [Conductor server issues](https://github.com/conductor-oss/conductor/issues) for OSS server behavior
- [Contributing](CONTRIBUTING.md) for local development, tests, and pull requests
- [Code of Conduct](CODE_OF_CONDUCT.md) for community expectations and conduct reporting
- [Security policy](SECURITY.md) for private vulnerability reporting
- [Conductor Slack](https://join.slack.com/t/orkes-conductor/shared_invite/zt-2vdbx239s-Eacdyqya9giNLHfrCavfaA) and the [Orkes Community Forum](https://community.orkes.io/) for questions

## License

Apache 2.0. See [LICENSE](LICENSE).
