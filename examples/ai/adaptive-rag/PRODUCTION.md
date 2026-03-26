# Adaptive RAG — Production Deployment Guide

## Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONDUCTOR_BASE_URL` | Yes | Conductor server API endpoint |
| `CONDUCTOR_OPENAI_API_KEY` | Yes | OpenAI API key for classification, reasoning, and generation |

All workers that call OpenAI (`ClassifyWorker`, `SimpleGenerateWorker`, `AnalyticalGenerateWorker`, `CreativeGenerateWorker`, `ReasoningWorker`) throw `IllegalStateException` at construction time if the key is missing.

## LLM Provider Setup

This example uses OpenAI `gpt-4o-mini` for all LLM calls. For production:

1. Create a project-scoped API key at https://platform.openai.com/api-keys
2. Set billing limits to prevent cost overruns
3. The ClassifyWorker uses `temperature=0.1` for deterministic classification
4. The CreativeGenerateWorker uses `temperature=0.9` for varied creative output
5. Consider using different models per path:
   - Classification: `gpt-4o-mini` (fast, cheap)
   - Simple generation: `gpt-4o-mini`
   - Analytical generation: `gpt-4o` (higher quality reasoning)
   - Creative generation: `gpt-4o` (better creative output)

## Classification Criteria

The ClassifyWorker routes queries to one of three paths:

| Type | Criteria | Pipeline |
|------|----------|----------|
| **factual** | Simple lookups, definitions, "what is", "who is" | SimpleRetrieve -> SimpleGenerate |
| **analytical** | Comparisons, trade-offs, multi-step reasoning, pros/cons | MultiHopRetrieve -> Reasoning -> AnalyticalGenerate |
| **creative** | Stories, poems, brainstorming, hypotheticals | CreativeGenerate (no retrieval) |

## Security

- Store API keys in a secrets manager
- Rotate keys on a 90-day schedule
- Use network restrictions to limit outbound traffic to `api.openai.com`

## Error Handling

- **Retryable** (`FAILED`): HTTP 429 (rate limit), HTTP 503 (overloaded)
- **Terminal** (`FAILED_WITH_TERMINAL_ERROR`): HTTP 400/401/403, missing/invalid inputs

## Deployment

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests
java -jar target/adaptive-rag-1.0.0.jar
```

## Monitoring

- Track classification distribution (factual/analytical/creative ratio)
- Alert on classification failures (may indicate prompt drift)
- Monitor per-path latency: creative is fastest (no retrieval), analytical is slowest (multi-hop + reasoning)
- Track token usage per generation worker for cost allocation
