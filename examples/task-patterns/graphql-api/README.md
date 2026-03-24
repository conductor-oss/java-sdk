# Graphql API in Java with Conductor

GraphQL API demo. single task workflow to demonstrate REST vs GraphQL query patterns.

## The Problem

You need to trigger a deployment operation through a GraphQL API. sending a project name and target environment, and getting back a structured deployment result. Unlike REST endpoints where you might need multiple calls to different URLs, a GraphQL query lets the client specify exactly what data it needs in a single request. The workflow takes the project and environment as inputs, executes the deployment via a GraphQL mutation or query, and returns the result.

Without orchestration, you'd call the GraphQL endpoint directly from your application code, handling authentication, query construction, error parsing, and retries inline. If the GraphQL API returns a partial error (some fields resolved, others failed), you'd need custom logic to decide whether to retry. There is no audit trail of which deployments were triggered, what inputs were sent, or what the API returned.

## The Solution

**You just write the GraphQL query construction and execution worker. Conductor handles retries, timeout management, and audit logging.**

This example demonstrates using a Conductor worker to interact with a GraphQL API. The GraphqlTaskWorker receives a project name and target environment, constructs the appropriate GraphQL query or mutation, executes the deployment operation, and returns the structured result. Conductor wraps the GraphQL call with automatic retries (handling transient network failures or API rate limits), timeout management, and full observability. every deployment request and response is recorded with timing data. This pattern works for any GraphQL API interaction, not just deployments.

### What You Write: Workers

A single GraphqlTaskWorker receives a project name and target environment, constructs the GraphQL query, executes the deployment operation, and returns the structured result.

| Worker | Task | What It Does |
|---|---|---|
| **GraphqlTaskWorker** | `gql_task` | GraphQL task worker. simulates a deployment operation. Takes a project name and environment, returns a deployment re.. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
gql_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
