# Examples

This module provides examples demonstrating how to use the Conductor Java SDK to create and run workflows, tasks, and workers.

## Prerequisites

- Java 21 or higher
- A running Conductor server ([start one with the CLI](https://conductor-oss.org))

## Running Examples

Set `CONDUCTOR_SERVER_URL` to point at your Conductor server (defaults to `http://localhost:8080/api`):

```bash
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
```

### Hello World (recommended starting point)

Registers a `greetings` workflow, starts it, and waits for completion:

```bash
./gradlew :examples:run
```

### Other examples

Pass a fully-qualified class name with `-PmainClass`:

```bash
# Getting-started examples (OSS-compatible, no auth required)
./gradlew :examples:run -PmainClass=com.netflix.conductor.gettingstarted.CreateWorkflow
./gradlew :examples:run -PmainClass=com.netflix.conductor.gettingstarted.StartWorkflow
./gradlew :examples:run -PmainClass=com.netflix.conductor.gettingstarted.HelloWorker

# Orkes-specific examples (require CONDUCTOR_AUTH_KEY + CONDUCTOR_AUTH_SECRET)
./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.WorkflowManagement
./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.MetadataManagement
```

> **Note:** `CreateWorkflow` registers the workflow definition. `StartWorkflow` launches an instance. `HelloWorker` polls and executes tasks. Run `HelloWorker` while `StartWorkflow` is running to see the workflow complete.

### Orkes Conductor (authenticated)

Examples under `io.orkes.*` connect to Orkes Conductor and require credentials:

```bash
export CONDUCTOR_SERVER_URL=https://your-cluster.orkesconductor.com/api
export CONDUCTOR_AUTH_KEY=your-key-id
export CONDUCTOR_AUTH_SECRET=your-key-secret
./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.WorkflowManagement
```
