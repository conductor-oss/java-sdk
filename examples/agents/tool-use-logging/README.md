# Tool Use Logging in Java Using Conductor : Request Log, Execute, Response Log, Audit Trail

Tool Use Logging: log tool requests and responses, execute tools, and create audit entries through a sequential pipeline. ## Every Tool Call Needs an Audit Trail

When an AI agent calls tools on behalf of users, you need to know exactly what was called, with what arguments, what it returned, how long it took, and who initiated it. Compliance (SOC2, HIPAA) requires audit logs. Debugging requires request-response pairs. Cost tracking requires knowing which tools are called how often.

The logging pattern wraps every tool call with pre-execution and post-execution logging. Before execution: log the tool name, arguments, user ID, and session ID. After execution: log the result, execution time, and any errors. The audit entry combines both into a single record that links the request to the response with timing data.

## The Solution

**You write the request logging, tool execution, response logging, and audit creation logic. Conductor handles the logging pipeline, ensuring audit records are created even on tool failure.**

`LogRequestWorker` records the incoming tool call. tool name, arguments, user ID, session ID, and timestamp, before execution begins. `ExecuteToolWorker` runs the actual tool and returns results. `LogResponseWorker` records the tool's output, execution duration, and success/error status. `CreateAuditEntryWorker` assembles the request log, response log, and timing data into a complete audit record. Conductor chains these four steps, ensuring logging happens even if the tool fails, and records the entire audit chain.

### What You Write: Workers

Four workers wrap tool calls with observability. Logging the request, executing the tool, logging the response, and creating a complete audit entry.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAuditEntryWorker** | `tl_create_audit_entry` | Creates an audit trail entry for a tool invocation. Takes requestId, userId, sessionId, toolName, toolArgs, result, e... |
| **ExecuteToolWorker** | `tl_execute_tool` | Simulates executing a tool (sentiment analysis). Takes toolName, toolArgs, requestId and returns a fixed sentiment an... |
| **LogRequestWorker** | `tl_log_request` | Logs an incoming tool request. Takes toolName, toolArgs, userId, sessionId and returns a fixed requestId, timestamp, ... |
| **LogResponseWorker** | `tl_log_response` | Logs a tool response. Takes requestId, toolName, result, executionTimeMs, toolStatus and returns a fixed timestamp an... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tl_log_request
 │
 ▼
tl_execute_tool
 │
 ▼
tl_log_response
 │
 ▼
tl_create_audit_entry

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
