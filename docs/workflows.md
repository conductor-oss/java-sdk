# Workflows

The fluent Workflow SDK creates typed Conductor workflow definitions in Java. Start with the maintained [workflow and worker Hello World](../examples/basics/hello-world/README.md); it registers a task definition, starts a worker, runs a workflow, and prints `Result: PASSED`.

## Prerequisites

- Java 21+
- A running Conductor server ([start one locally with the CLI](server-setup.md), or use an existing server)
- `org.conductoross:conductor-client:<VERSION>` from [Maven Central](https://search.maven.org/search?q=g:org.conductoross)

`SIMPLE` tasks require both a registered task definition and a worker polling the exact task name. The Hello World example is the shortest complete reference for that lifecycle.

## Build a workflow

**Fragment — create this after constructing a `WorkflowExecutor` for your server.**

```java
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.WorkflowBuilder;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

WorkflowExecutor executor = new WorkflowExecutor("http://localhost:8080/api");

ConductorWorkflow<java.util.Map<String, Object>> workflow =
        new WorkflowBuilder<java.util.Map<String, Object>>(executor)
                .name("quote_workflow")
                .version(1)
                .description("Calculate and email an insurance quote")
                .ownerEmail("team@example.com")
                .timeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF, 300)
                .add(
                        new SimpleTask("calculate_quote", "calculate_quote"),
                        new SimpleTask("send_email", "send_email")
                                .input("email", "${workflow.input.email}")
                                .input("subject", "Your quote is ${calculate_quote.output.amount}"))
                .build();
```

`ConductorWorkflow` can register a reusable definition, execute an already registered definition, or submit a one-off dynamic definition:

```java
workflow.registerWorkflow(true, true);             // overwrite and register missing task definitions
workflow.execute(java.util.Map.of("email", "user@example.com"));
workflow.executeDynamic(java.util.Map.of("email", "user@example.com"));
```

Use dynamic execution only when each run truly needs a distinct graph; registered definitions are easier to inspect, reuse, and evolve.

## System tasks

The builder accepts all task classes in `com.netflix.conductor.sdk.workflow.def.tasks`, including `ForkJoin`, `Wait`, `Switch`, `DynamicFork`, `DoWhile`, `Join`, `Dynamic`, `Terminate`, `SubWorkflow`, and `SetVariable`.

Read the current source when you need a task-specific builder: [workflow task classes](https://github.com/conductor-oss/java-sdk/tree/main/conductor-client/src/main/java/com/netflix/conductor/sdk/workflow/def/tasks).

## AI and vector tasks

The fluent SDK exposes current Conductor system-task types. Provider and vector database integrations are configured on the server.

**Fragment — index text:**

```java
import org.conductoross.conductor.sdk.ai.LlmIndexText;

LlmIndexText indexText = new LlmIndexText("index_doc", "index_ref")
        .vectorDb("knowledge_base")
        .namespace("documents")
        .index("articles")
        .embeddingModelProvider("openai")
        .embeddingModel("text-embedding-3-small")
        .text("${workflow.input.document}")
        .docId("${workflow.input.documentId}");
```

**Fragment — search the index:**

```java
import org.conductoross.conductor.sdk.ai.LlmSearchIndex;

LlmSearchIndex search = new LlmSearchIndex("search_docs", "search_ref")
        .vectorDb("knowledge_base")
        .namespace("documents")
        .index("articles")
        .embeddingModelProvider("openai")
        .embeddingModel("text-embedding-3-small")
        .query("${workflow.input.question}")
        .maxResults(5);
```

Add either task with `.add(indexText)` or `.add(search)`. The exact embedding model must match the model used to index the data.

## Related guides

- [Workers](workers.md) — implement and run `SIMPLE` tasks.
- [Workflow test harness](workflow-testing.md) — test definitions and workers against a local server.
- [Agent SDK](agents/README.md) — build durable LLM agents, tools, and dynamic plans.
