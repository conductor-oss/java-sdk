# Classifying Support Tickets Through Amazon Bedrock

An enterprise support system needs to classify incoming tickets for urgency and compliance risk. The classification must go through Amazon Bedrock's InvokeModel API using Claude on Bedrock, with the Anthropic Messages API payload format, and the response needs to be parsed from Bedrock's specific envelope structure.

## Workflow

```
prompt, useCase
       │
       ▼
┌─────────────────────┐
│ bedrock_build_payload│  Build Anthropic Messages API body
└─────────┬───────────┘
          │  payload, modelId, region
          ▼
┌─────────────────────┐
│ bedrock_invoke_model │  Call Bedrock InvokeModel
└─────────┬───────────┘
          │  responseBody (Claude-on-Bedrock format)
          ▼
┌─────────────────────┐
│ bedrock_parse_output │  Extract classification text
└─────────────────────┘
          │
          ▼
   classification, modelId, latency, tokens
```

## Workers

**BedrockBuildPayloadWorker** (`bedrock_build_payload`) -- Assembles the Bedrock-specific request body. Sets `anthropic_version` to `"bedrock-2023-05-31"`, injects `maxTokens` and `temperature` from workflow input, and wraps the user prompt with a `"Use case: " + useCase` prefix inside a single-message `messages` array. The workflow hardcodes `modelId` to `anthropic.claude-3-sonnet-20240229-v1:0`, region to `us-east-1`, `maxTokens` to 512, and `temperature` to 0.5.

**BedrockInvokeModelWorker** (`bedrock_invoke_model`) -- Checks for `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables at construction time via `System.getenv()`. In production, this is where you'd wire in the AWS SDK v2 `BedrockRuntimeClient.invokeModel()` call. The deterministic fallback returns a response with `stop_reason: "end_turn"`, a `usage` map of 67 input / 89 output tokens, and a classification of "URGENT -- Compliance Risk" referencing incident playbook `IR-003` and GDPR Article 33. Tracks `latencyMs` (1850) in a metrics map.

**BedrockParseOutputWorker** (`bedrock_parse_output`) -- Navigates the Claude-on-Bedrock response structure: casts `responseBody` to `Map`, extracts `content` as a `List<Map>`, and pulls `.get(0).get("text")`. Logs the first 45 characters of the extracted classification via `Math.min(45, text.length())`.

## Tests

13 tests across 3 test files cover payload construction, model invocation in fallback mode, and response parsing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
