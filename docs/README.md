# Java SDK documentation

Build durable workflow workers and AI agents with Conductor. This repository documents both **OSS** and **Orkes** in place; pages call out capabilities that need Orkes.

## Start here

| Goal | Guide | Expected result |
|---|---|---|
| Connect to a server | [Server setup](server-setup.md) and [connection/authentication](connection-authentication.md) | A local or remote API endpoint accepts SDK requests. |
| Build a workflow and Java worker | [Core quickstart](core-quickstart.md) | Hello World prints `Result: PASSED`. |
| Build an AI agent | [Agent quickstart](agents/getting-started.md) | An LLM-backed agent completes through Conductor. |

## Build

- [Workflows](workflows.md) and [workflow lifecycle](workflow-lifecycle.md)
- [Workers](workers.md), [workflow testing](workflow-testing.md), [files](file-client.md), and [schemas](schema-client.md)
- [Schedules and events](schedules-events.md)
- [AI agents](agents/README.md), [tools](agents/concepts/tools.md), and [agent framework bridges](agents/README.md#framework-bridges)
- [Curated examples](examples.md); the [complete catalog](../examples/README.md) remains machine-generated.

## Operate

- [Reliability: timeouts, retries, idempotency, and domains](reliability.md)
- [Security and secrets](security.md)
- [Deployment, scaling, and graceful shutdown](deployment-scaling.md)
- [Metrics and logging](observability.md)
- [Debugging incidents](debugging.md)

## Integrate

- [Spring Boot integration selector](spring-boot.md)
- [Spring Boot 3 module](../conductor-client-spring/README.md)
- [Spring Boot 4 module](../conductor-client-spring-boot4/README.md)
- [Google ADK](agents/frameworks/google-adk.md), [LangChain4j](agents/frameworks/langchain4j.md), and [LangGraph4j](agents/frameworks/langgraph4j.md)

## Reference and upgrades

- [API map and generated Javadocs](api-map.md)
- [Compatibility matrix](compatibility.md)
- [Upgrade guide](upgrading.md)
- [Agent client control plane](agents/reference/client.md), [agent runtime](agents/reference/runtime.md), and [agent schema](agents/reference/agent-schema.md)

## Documentation conventions

- Replace `<VERSION>` with a published SDK version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).
- Runnable commands link to maintained examples and state an expected result. Short Java blocks labeled **Fragment** need surrounding application setup and link to a complete path.
- Provider credentials belong on the **Conductor server**, not only in the Java client process. Never place secrets in workflow input or source control.
- Primary guide authors follow the [documentation standard](documentation-standard.md).
