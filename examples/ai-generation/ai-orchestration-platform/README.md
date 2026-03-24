# AI Request Orchestration: Receive, Route to Model, Execute, Validate, Respond

An AI orchestration platform receives a request, routes it to the appropriate model endpoint (`http://models/text-model-v3/predict`), executes the inference, validates the response quality (coherence, relevance, safety score 0.0-1.0 via LLM), and sends the response.

## Workflow

```
requestType, payload, priority
  -> aop_receive_request -> aop_route_model -> aop_execute -> aop_validate -> aop_respond
```

## Tests

2 tests cover the orchestration pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
