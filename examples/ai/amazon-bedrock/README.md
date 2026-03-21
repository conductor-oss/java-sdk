# Amazon Bedrock Integration in Java Using Conductor : Build Payload, Invoke Model, Parse Output

## Calling Bedrock Models Reliably in Production

Amazon Bedrock provides access to foundation models from AI21, Anthropic, Cohere, Meta, and Stability AI. But calling `InvokeModel` in production means more than a single API call. you need to construct the model-specific payload format (Claude uses `messages`, Titan uses `inputText`), handle throttling and quota errors with retry logic, parse the model-specific response format, and log the prompt, response, and latency for cost tracking and debugging.

Without orchestration, the payload construction, API call, and response parsing get tangled in a single method. When you switch from Claude to Titan, you change the payload format and break the parser. When Bedrock throttles you, the retry logic is mixed in with the business logic.

## The Solution

**You write the Bedrock payload construction and response parsing. Conductor handles the invocation pipeline, retries, and observability.**

`BedrockBuildPayloadWorker` constructs the model-specific request body based on the prompt and use case. selecting the right payload format, temperature, and max tokens for the target model. `BedrockInvokeModelWorker` calls the Bedrock `InvokeModel` API and handles throttling/quota responses. `BedrockParseOutputWorker` extracts the generated text from the model-specific response format and structures it for downstream use. Conductor retries throttled invocations with backoff, and records every prompt, model response, and latency for cost analysis and debugging.

### What You Write: Workers

Three workers separate the Bedrock integration into payload construction, model invocation, and response parsing. isolating the model-specific formats from the orchestration logic.

| Worker | Task | What It Does |
|---|---|---|
| **BedrockBuildPayloadWorker** | `bedrock_build_payload` | Builds the Bedrock InvokeModel payload for Claude on Bedrock. |
| **BedrockInvokeModelWorker** | `bedrock_invoke_model` | Simulates calling Amazon Bedrock InvokeModel API. In production, this would use the AWS SDK BedrockRuntimeClient to c... |
| **BedrockParseOutputWorker** | `bedrock_parse_output` | Parses the Bedrock response to extract the classification text. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
bedrock_build_payload
 │
 ▼
bedrock_invoke_model
 │
 ▼
bedrock_parse_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
