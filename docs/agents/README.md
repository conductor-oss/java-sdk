# Conductor Java Agent SDK

Build durable Java AI agents on Conductor. Agents can use local Java tools, wait for people, execute dynamic plans, and recover after a process restart because Conductor persists execution state.

**New here?** Follow [Getting Started](getting-started.md) to configure a server-side LLM provider and run the maintained basic-agent example.

For an AI coding agent that understands Conductor operations, load [Conductor Skills](https://github.com/conductor-oss/conductor-skills): `npm install -g @conductor-oss/conductor-skills && conductor-skills --all`.

## Install

Requirements: Java 21+ and a Conductor server.

```groovy
dependencies {
    implementation 'org.conductoross:conductor-client-ai:<VERSION>'
}
```

Replace `<VERSION>` with a published version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).

## Start here

- **[Getting Started](getting-started.md)** — configure a server and run a maintained agent example.
- **[Deploy · Serve · Run · Plan](concepts/deploy-serve-run.md)** — choose the right runtime mode.
- **[Scheduling](concepts/scheduling.md)** — schedule deployed agents with the shared typed client.

## Build agents

- **[Agents](concepts/agents.md)** — builder API and `@AgentDef`.
- **[Tools](concepts/tools.md)** — Java tools, HTTP/MCP tools, human approval, files, and credentials.
- **[Multi-Agent](concepts/multi-agent.md)** — sequential, parallel, handoff, swarm, and plan-execute agents.
- **[Guardrails](concepts/guardrails.md)**, **[Termination](concepts/termination.md)**, **[Callbacks](concepts/callbacks.md)**, **[Stateful Agents](concepts/stateful.md)**, **[Streaming & Human-in-the-Loop](concepts/streaming-hitl.md)**, and **[Structured Output](concepts/structured-output.md)**.

## Framework bridges

- **[Google ADK](frameworks/google-adk.md)** — bridge native ADK agents and sub-agent graphs.
- **[LangChain4j](frameworks/langchain4j.md)** — turn existing `@Tool` POJOs and `ChatModel` metadata into Conductor agents.
- **[LangGraph4j](frameworks/langgraph4j.md)** — run a native graph builder on the durable runtime.
- **[OpenAI Agents SDK style](frameworks/openai.md)** — use familiar tool and handoff shapes in Java.

## Operate and inspect

- **[Spring Boot](spring-boot.md)** — auto-configured runtime and `@AgentDef` discovery.
- **[Runtime reference](reference/runtime.md)** and **[control-plane reference](reference/client.md)**.
- **[API map](reference/api.md)**, [agent-definition fields](reference/agent-definition.md), and [configuration schema](reference/agent-schema.md).
- [Control-plane example](../../agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example71AgentControlPlane.java) — deploy, start, inspect, stop, and cancel an agent.

## What Conductor adds

| Capability | Conductor agent runtime |
|---|---|
| Process failure recovery | Durable workflow state resumes from completed work. |
| Java tools | Tools run as independently scalable Conductor worker tasks. |
| Long-running work | Human approval, schedules, and events do not hold application threads open. |
| Dynamic execution | Plans become durable sub-workflows that can be inspected and retried. |
| Observability | Inputs, outputs, tool calls, retries, and status share one execution record. |
