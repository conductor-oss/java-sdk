# AI Orchestration Platform in Java Using Conductor : Receive, Route to Model, Execute, Validate, Respond

## Routing AI Requests to the Right Model

An organization running multiple AI models (GPT-4 for complex reasoning, Claude for long documents, a fine-tuned model for domain-specific tasks, a local model for sensitive data) needs a routing layer that sends each request to the right model. A summarization request goes to the model best at summarization. A coding request goes to the model best at code. A request involving PII goes to the on-premises model.

The routing decision depends on request type, data sensitivity, model availability, cost constraints, and priority level. After model execution, the response needs quality validation before returning to the caller. catching model hallucinations, format violations, and incomplete responses. Without orchestration, this routing becomes hardcoded conditional logic that's impossible to update when you add new models or change routing rules.

## The Solution

**You just write the request intake, model routing, inference execution, response validation, and delivery logic. Conductor handles model routing, inference retries, and end-to-end request tracking across providers.**

`ReceiveRequestWorker` ingests the AI request and extracts its type, priority, and data sensitivity classification. `RouteModelWorker` selects the best model based on request type, sensitivity constraints, model availability, and cost/priority trade-offs. `ExecuteWorker` sends the request to the selected model and captures the response with usage metadata. `ValidateWorker` checks response quality. format compliance, completeness, and confidence thresholds. `RespondWorker` returns the validated response to the caller with model selection metadata. Conductor tracks which model handled each request type for routing optimization.

### What You Write: Workers

Request routing, model execution, and response validation each run as isolated workers, letting you add new models without changing the orchestration layer.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteWorker** | `aop_execute` | Executes model inference at the selected endpoint, returning the result with latency and token usage metrics |
| **ReceiveRequestWorker** | `aop_receive_request` | Ingests the incoming AI request and assigns a request ID based on type and priority |
| **RespondWorker** | `aop_respond` | Sends the orchestrated response back to the requester with a status code and confirmation |
| **RouteModelWorker** | `aop_route_model` | Routes the request to the best model (e.g., text-model-v3) based on request type, selecting the model endpoint with load balancing |
| **ValidateWorker** | `aop_validate` | Validates the model response for coherence, relevance, and safety. assigns a quality score and safety flag |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
aop_receive_request
 │
 ▼
aop_route_model
 │
 ▼
aop_execute
 │
 ▼
aop_validate
 │
 ▼
aop_respond

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
