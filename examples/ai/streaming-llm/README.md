# Streaming LLM Responses: Prepare, Collect Chunks, Post-Process

Streaming LLM responses arrive as chunks that need to be collected into a complete response, then post-processed (word count, formatting). This pipeline prepares the request, collects all streamed chunks into a single response, and runs post-processing.

## Workflow

```
prompt, model, maxTokens
       │
       ▼
┌──────────────────────┐
│ stream_prepare       │  Build streaming request
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ stream_collect_chunks│  Collect all SSE chunks
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ stream_post_process  │  Word count + formatting
└──────────────────────┘
```

## Workers

**StreamPrepareWorker** (`stream_prepare`) -- Prepares the streaming request parameters.

**StreamCollectChunksWorker** (`stream_collect_chunks`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `https://api.openai.com/v1/chat/completions` with `stream: true` and collects SSE chunks. In fallback mode, returns `DEFAULT_CHUNKS` -- a predefined list of text fragments that simulate streaming output.

**StreamPostProcessWorker** (`stream_post_process`) -- Joins all chunks into `fullResponse` and computes `wordCount` via `fullResponse.split("\\s+").length`.

## Tests

14 tests cover stream preparation, chunk collection in both modes, and post-processing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
