# AI Guardrails: Input Check, Content Filter, Generate, Output Check, Deliver

Five safety layers wrap the LLM call: input validation checks the user prompt for safety, content filtering screens for policy violations, the LLM generates the response, output checking evaluates toxicity/hallucination/safety scores, and delivery sends the vetted response.

## Workflow

```
userPrompt, userId, modelId
  -> grl_input_check -> grl_content_filter -> grl_generate -> grl_output_check -> grl_deliver
```

## Workers

**InputCheckWorker** (`grl_input_check`) -- Checks prompt safety via LLM.

**ContentFilterWorker** (`grl_content_filter`) -- Screens for policy violations via LLM.

**GenerateWorker** (`grl_generate`) -- The actual LLM generation step.

**OutputCheckWorker** (`grl_output_check`) -- Evaluates: `"Check the following AI-generated response for safety... Provide toxicity score, hallucination score, and safety determination."`

**DeliverWorker** (`grl_deliver`) -- Delivers the vetted response.

## Tests

2 tests cover the guardrails pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
