# Tool Use Error Handling in Java Using Conductor : Primary Tool with Fallback on Failure

Tool Use Error Handling. tries a primary tool and falls back to an alternative tool on failure via a SWITCH task. ## Tools Fail : Have a Backup Plan

Your primary weather API returns a 503 because it's having an outage. Your primary search engine is rate-limiting you. Your primary database is under maintenance. If the agent simply reports "tool failed" to the user, it's a poor experience. especially when an alternative tool could have answered the question.

The error handling pattern tries the primary tool first, checks its status, and on failure routes to a fallback tool that serves the same purpose through a different provider or method. The primary might be a paid, high-quality API; the fallback might be a free, lower-quality alternative. Either way, the user gets an answer. Conductor's `SWITCH` task makes this failover routing explicit, and every execution records which tool served the request.

## The Solution

**You write the primary and fallback tool logic. Conductor handles the success/failure routing, failover decisions, and reliability tracking per tool.**

`TryPrimaryToolWorker` executes the preferred tool and returns success/failure status with the result or error details. Conductor's `SWITCH` routes on status: success goes to `FormatSuccessWorker` which formats the primary tool's output. Failure (the default case) goes to `TryFallbackToolWorker` which executes the alternative tool, then `FormatFallbackWorker` which formats the fallback result with a note about which tool was used. Conductor records which tool served each request, enabling reliability analysis per tool.

### What You Write: Workers

Four workers implement failover. Trying the primary tool, checking its status, and routing to a fallback tool on failure before formatting the result.

| Worker | Task | What It Does |
|---|---|---|
| **FormatFallbackWorker** | `te_format_fallback` | Formats the result from a successful fallback tool invocation. |
| **FormatSuccessWorker** | `te_format_success` | Formats the result from a successful primary tool invocation. |
| **TryFallbackToolWorker** | `te_try_fallback_tool` | Attempts the fallback tool after the primary tool has failed. Returns a successful geocoding result. |
| **TryPrimaryToolWorker** | `te_try_primary_tool` | Attempts to call the primary tool. Simulates a failure by returning toolStatus="failure" with a 503 service-unavailab... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
te_try_primary_tool
 │
 ▼
SWITCH (route_on_status_ref)
 ├── success: te_format_success
 └── default: te_try_fallback_tool -> te_format_fallback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
