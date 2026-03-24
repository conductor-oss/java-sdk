# AI Orchestration Platform: Receive, Route, Execute, Validate, Respond

An AI platform receives inference requests of different types -- text summarization, image classification, code completion -- and needs to route each request to the appropriate model endpoint. Sending a text request to an image model wastes compute and returns garbage. The platform also needs to validate the model's output for quality, relevance, and safety before returning it to the caller.

This workflow receives a request, routes it to the best model, executes inference, validates the output, and responds.

## Pipeline Architecture

```
requestType, payload, priority
         |
         v
  aop_receive_request    (requestId="REQ-ai-orchestration-platform-001")
         |
         v
  aop_route_model        (selectedModel="text-model-v3", modelEndpoint, loadBalanced)
         |
         v
  aop_execute            (result text, latencyMs=145, tokensUsed=24)
         |
         v
  aop_validate           (validatedResult, quality=0.95, safe=true)
         |
         v
  aop_respond            (responded=true, statusCode=200)
```

## Worker: ReceiveRequest (`aop_receive_request`)

Accepts the incoming request and assigns a deterministic request ID: `"REQ-ai-orchestration-platform-001"`. The `requestType` and `priority` inputs are passed through to downstream workers for routing decisions.

## Worker: RouteModel (`aop_route_model`)

Selects the appropriate model based on the `requestType`. Returns `selectedModel: "text-model-v3"`, `modelEndpoint: "http://models/text-model-v3/predict"`, and `loadBalanced: true`. The endpoint URL is constructed from the model name.

## Worker: Execute (`aop_execute`)

Sends the payload to the model endpoint for inference. When an LLM is available, executes real inference and reports actual latency and token usage. Falls back to a deterministic result: `"The article discusses advances in renewable energy technology."` with `latencyMs: 145` and `tokensUsed: 24`.

## Worker: Validate (`aop_validate`)

Validates the inference result for coherence, relevance, and safety. When an LLM is available, delegates quality assessment to the model and returns detailed `validationDetails`. Falls back to `quality: 0.95`, `safe: true`, and passes through the `validatedResult` unchanged. Quality scores range from 0.0 to 1.0.

## Worker: Respond (`aop_respond`)

Sends the validated result back to the caller with the original `requestId` for correlation. Returns `responded: true` and `statusCode: 200`.

## Tests

2 tests cover the AI orchestration pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
