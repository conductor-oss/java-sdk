# GraphQL API

A deployment workflow queries a GraphQL endpoint for project configuration and deploys based on the response. The single worker handles the full deployment logic with project and environment context.

## Workflow

```
gql_task
```

Workflow `graphql_demo` accepts `project` and `env`. Times out after `60` seconds.

## Workers

**GraphqlTaskWorker** (`gql_task`) -- reads `project` and `env` from input. Reports deploying with the specified project and environment settings.

## Workflow Output

The workflow produces `project`, `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `gql_task`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `graphql_demo` defines 1 task with input parameters `project`, `env` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "GraphQL API demo — single task workflow to demonstrate REST vs GraphQL query patterns.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify GraphQL task execution, project and environment parameter handling, and deployment output.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
