# API Gateway in Java with Conductor

A mobile client hits your API. The request needs to be authenticated, routed to the right backend, and the response transformed before it goes back. You built this as a single Spring controller: authenticate inline, make the backend call, transform the response, return. Then the auth service starts taking 2 seconds instead of 200ms. Your entire gateway blocks. You add a retry loop for the backend call, but now a transient auth failure retries the backend too. A customer reports getting someone else's response, the response-transform step was reading from a shared variable that another thread overwrote. Every fix makes the controller harder to reason about, and when something fails, you have no idea which of the four steps caused it. This workflow breaks the gateway into independent, observable steps, authenticate, route, transform, respond, each with its own retries and timeouts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

An API gateway sits between external clients and your backend services. Every request must be authenticated, routed to the correct service, have its response transformed into a client-friendly format, and be sent back with appropriate headers. These steps are inherently sequential, routing depends on the authenticated client identity, and the response transform depends on the routing result.

Without orchestration, you wire authenticate-route-transform-respond into a single class with nested try/catch blocks, bespoke retry loops for each outbound call, and no visibility into where a request failed. Adding a new step (rate limiting, logging, caching) means rewriting the entire pipeline.

## The Solution

**You just write the authentication, routing, and response-transform workers. Conductor handles sequential execution, per-step retries, and full request tracing across the gateway pipeline.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The gateway pipeline breaks into four focused workers: authenticating API keys, routing to backends, transforming responses, and sending the final reply, each handling one hop in the request lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AgAuthenticateWorker** | `ag_authenticate` | Validates an API key and returns client info. |
| **RouteRequestWorker** | `ag_route_request` | Routes the API request to the appropriate backend service. |
| **SendResponseWorker** | `ag_send_response` | Sends the final API response to the client. |
| **TransformResponseWorker** | `ag_transform_response` | Transforms the raw backend response into a client-friendly format. |

### The Workflow

```
ag_authenticate
 │
 ▼
ag_route_request
 │
 ▼
ag_transform_response
 │
 ▼
ag_send_response

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
