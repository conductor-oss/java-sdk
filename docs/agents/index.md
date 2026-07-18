# Conductor Java Agent SDK

Build long-running, dynamic plan-execute, and event-driven AI agents in Java on Conductor. Your agents survive process crashes, run on cron, trigger from events, and execute dynamic plans deterministically — all without managing state yourself.

```java
Agent agent = Agent.builder()
    .name("assistant")
    .model("anthropic/claude-sonnet-4-6")
    .instructions("You are a helpful assistant.")
    .build();

try (AgentRuntime runtime = new AgentRuntime()) {
    AgentResult result = runtime.run(agent, "What is the capital of France?");
    System.out.println(result.getOutput());
}
```

Namespace: `org.conductoross.conductor.ai`. Requires Java 21+.

## Documentation map

The docs are organized into five areas:

### a) Get started

- **[Getting Started](getting-started.md)** — install (Maven/Gradle), set env vars, run your first agent in under 30 seconds.

### b) Writing agents

- **[Agents](concepts/agents.md)** — the full `Agent.builder()` API, dynamic instructions, `@AgentDef`/`Agent.fromInstance`.
- **[Tools](concepts/tools.md)** — `@Tool` + `ToolRegistry.fromInstance`, and built-ins: HTTP, MCP, Human, Media (image/audio/video), PDF, RAG, WaitForMessage, AgentTool.
- **[Multi-Agent](concepts/multi-agent.md)** — sequential, parallel, handoff, router, swarm, round-robin, plan-execute.
- **[Guardrails](concepts/guardrails.md)** · **[Termination](concepts/termination.md)** — validation and early-exit conditions.
- **[Callbacks](concepts/callbacks.md)** — lifecycle hooks (`CallbackHandler`).
- **[Streaming & Human-in-the-Loop](concepts/streaming-hitl.md)** — event streams and approval flows.
- **[Stateful Agents](concepts/stateful.md)** — sessions, conversation memory, multi-turn.
- **[Structured Output](concepts/structured-output.md)** — typed results via `outputType`.
- **[Scheduling](concepts/scheduling.md)** · **[Skills](concepts/skills.md)**.

### c) Framework agents

Run agents authored in another framework on the durable Conductor runtime.

- **[OpenAI Agents SDK](frameworks/openai.md)** · **[Google ADK](frameworks/google-adk.md)** · **[LangChain4j](frameworks/langchain4j.md)** · **[LangGraph4j](frameworks/langgraph4j.md)**.

### d) Operating agents

- **[Deploy · Serve · Run · Plan](concepts/deploy-serve-run.md)** — the four runtime modes.
- **[Spring Boot](spring-boot.md)** — auto-configuration and `@AgentDef` bean discovery.
- **[Agent Field Reference](agent-structure.md)** · **[Agent JSON Schema](agent-schema.md)** — the wire format.

### e) API reference

- **[Public API summary](api-reference.md)** — every public signature on one page.
- **[AgentRuntime](agent-runtime-api.md)** — the entry-point class in detail.
- **[AgentClient](agent-client-api.md)** — the `/api/agent/*` control plane: start deployed or inline agents, inspect status timestamps, respond, gracefully stop, or immediately cancel.
- **[Control-plane example](../../agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example71AgentControlPlane.java)** — runnable deployed-agent start/status/stop/cancel flow.

## Installation

=== "Gradle"

    ```groovy
    implementation 'org.conductoross:conductor-client-ai:5.1.0'
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>org.conductoross</groupId>
        <artifactId>conductor-client-ai</artifactId>
        <version>5.1.0</version>
    </dependency>
    ```

**Requirements:** Java 21+ · a Conductor server (see [Getting Started](getting-started.md)).

## What makes it different

| Feature | Conductor | Thread-based SDKs |
|---|---|---|
| Survives crashes | ✅ Conductor workflow | ❌ State lost |
| Tool workers | ✅ Distributed tasks | ❌ In-process only |
| Long-running | ✅ Days / weeks | ❌ Minutes |
| Human-in-the-loop | ✅ Native approval flow | ❌ Polling hacks |
| Observability | ✅ Full workflow audit log | ❌ Log scraping |
