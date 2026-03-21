# Streaming LLM in Java Using Conductor : Prepare, Collect Chunks, Post-Process

## Integrating Streaming LLMs into Workflows

LLM streaming APIs (SSE/WebSocket) deliver responses token by token for lower perceived latency. But workflow tasks need complete inputs and outputs. The solution: a worker that opens a streaming connection, collects all chunks into a buffer, and returns the complete response when the stream ends. This gives you streaming's benefits (early token delivery, chunked processing) while fitting into Conductor's request-response model.

If the stream breaks mid-generation, the worker can signal failure and Conductor retries the entire stream collection automatically.

## The Solution

**You write the stream collection and chunk assembly logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each concern is an independent worker. stream preparation (building the request with streaming enabled), chunk collection (consuming SSE events and assembling the complete response), and post-processing (formatting, token counting, or content filtering). Conductor sequences them and retries the chunk collector if the stream disconnects mid-generation.

### What You Write: Workers

Three workers handle LLM streaming. preparing the SSE connection parameters, collecting streamed chunks into a complete response, and post-processing the assembled text with token counts and timing.

| Worker | Task | What It Does |
|---|---|---|
| **StreamCollectChunksWorker** | `stream_collect_chunks` | Simulates collecting streamed chunks from an LLM and assembling them into a full response. |
| **StreamPostProcessWorker** | `stream_post_process` | Post-processes the assembled LLM response: counts words and marks the result as processed. |
| **StreamPrepareWorker** | `stream_prepare` | Formats a raw prompt into a system/user prompt pair for the LLM. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
stream_prepare
 │
 ▼
stream_collect_chunks
 │
 ▼
stream_post_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
