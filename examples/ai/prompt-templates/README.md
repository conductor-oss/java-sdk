# Versioned Prompt Templates with Variable Resolution

Prompt engineering needs version control. When you change a prompt, you need to A/B test the old and new versions, roll back if the new one performs worse, and track which version produced which output. This workflow resolves a versioned template from a store, substitutes variables, calls the LLM, and collects the result with metadata.

## Workflow

```
templateId, templateVersion, variables, model
       │
       ▼
┌──────────────────────────┐
│ pt_resolve_template      │  Look up template + substitute variables
└───────────┬──────────────┘
            │  resolvedPrompt, templateKey
            ▼
┌──────────────────────────┐
│ pt_call_llm              │  Call OpenAI with resolved prompt
└───────────┬──────────────┘
            │  response, tokens
            ▼
┌──────────────────────────┐
│ pt_collect               │  Log and collect results
└──────────────────────────┘
            │
            ▼
     response, templateKey, tokens
```

## Workers

**ResolveTemplateWorker** (`pt_resolve_template`) -- Looks up templates from a static `TEMPLATE_STORE` keyed by `"templateId:vVersion"`. Contains two templates: `"summarize:v2"` with pattern `"Summarize the following {{format}}:\n\nTopic: {{topic}}\nAudience: {{audience}}\n\nProvide a {{length}} summary."` and `"classify:v1"` with pattern `"Classify the following text into one of these categories: {{categories}}.\n\nText: {{text}}\n\nCategory:"`. Substitutes each variable by calling `resolved.replace("{{" + key + "}}", value)` for each entry in the `variables` map. Returns FAILED with `"Template not found: <key>"` if the template doesn't exist.

**CallLlmWorker** (`pt_call_llm`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls OpenAI with the resolved prompt. In fallback mode, returns a `FIXED_RESPONSE` with `tokens: 42`.

**CollectWorker** (`pt_collect`) -- Logs the result and returns `logged: true`.

## Tests

13 tests cover template resolution, variable substitution, missing template handling, and LLM calling.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
