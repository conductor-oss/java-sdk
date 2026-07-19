# Java SDK documentation

Use this guide to get a successful first result, then move into the API reference that fits your application.

For a local server, start with the [Conductor CLI setup](server-setup.md). Docker is available when a containerized server is required. If you use an AI coding agent, load [Conductor Skills](https://github.com/conductor-oss/conductor-skills) with `npm install -g @conductor-oss/conductor-skills && conductor-skills --all`.

## Choose a path

| Goal | Start here | What you will prove |
|---|---|---|
| Build a durable AI agent | [Run your first agent](agents/getting-started.md) | An LLM-backed agent completes through Conductor. |
| Build a workflow and Java worker | [Run Hello World](../examples/basics/hello-world/README.md) | A workflow dispatches a `SIMPLE` task to a Java worker. |
| Define workflows in Java | [Workflows](workflows.md) | Fluent workflow definitions and system tasks. |
| Implement workers | [Workers](workers.md) | Interface and annotation-based workers. |
| Test workflows against a local server | [Workflow test harness](workflow-testing.md) | A workflow and its workers execute in a test. |

## After your first result

- **AI agents:** [agent guide](agents/README.md), [tools](agents/concepts/tools.md), [framework bridges](agents/README.md#framework-bridges), and [scheduling](agents/concepts/scheduling.md).
- **Core client:** [Schema client](schema-client.md), [FileClient](file-client.md), and the [examples catalog](../examples/README.md).
- **Design contracts:** [file transfer](../design/file-client.md) and [tool credential delivery](../design/secret-injection-contract.md).

## Documentation conventions

- Replace `<VERSION>` with a published SDK version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).
- A `provider/model` value is an example; the provider credential must be configured on the **Conductor server**, not only in the Java client process.
- Runnable commands link to maintained examples. Short Java blocks labeled **Fragment** show one API concept and need surrounding application setup.
