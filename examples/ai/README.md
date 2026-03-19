# AI / LLM Examples

These examples demonstrate LLM integration patterns with Conductor workflows: prompt chaining, RAG pipelines, multi-provider fallbacks, cost tracking, and structured output parsing.

## Prerequisites

1. **Running Conductor server**:
   ```bash
   npm install -g @conductor-oss/conductor-cli
   conductor server start
   ```

2. **Java 21+** and **Maven 3.8+**

3. **LLM API key** (see table below). `first-ai-workflow` runs in **simulated mode by default** (no API key needed). Other examples require an API key — set the appropriate environment variable before running.

## Examples

| Example | Description | Environment Variable |
|---|---|---|
| `first-ai-workflow` | Simplest 3-step LLM pipeline: prepare prompt, call model, parse response | `CONDUCTOR_OPENAI_API_KEY` |
| `openai-gpt4` | OpenAI GPT-4 integration | `CONDUCTOR_OPENAI_API_KEY` |
| `anthropic-claude` | Anthropic Claude integration | `CONDUCTOR_ANTHROPIC_API_KEY` |
| `google-gemini` | Google Gemini integration | `CONDUCTOR_GOOGLE_API_KEY` |
| `llm-chain` | Chain multiple LLM calls sequentially | `CONDUCTOR_OPENAI_API_KEY` |
| `llm-fallback-chain` | Try primary LLM, fall back to secondary on failure | `CONDUCTOR_OPENAI_API_KEY` |
| `llm-cost-tracking` | Track token usage and cost across LLM calls | `CONDUCTOR_OPENAI_API_KEY` |
| `structured-output` | Parse LLM responses into typed fields | `CONDUCTOR_OPENAI_API_KEY` |
| `basic-rag` | Retrieval-Augmented Generation basics | `CONDUCTOR_OPENAI_API_KEY` |
| `adaptive-rag` | RAG with adaptive retrieval strategy | `CONDUCTOR_OPENAI_API_KEY` |
| `corrective-rag` | RAG with self-correction on low-confidence answers | `CONDUCTOR_OPENAI_API_KEY` |
| `rag-citation` | RAG with source citation tracking | `CONDUCTOR_OPENAI_API_KEY` |
| `rag-hybrid-search` | RAG combining keyword and vector search | `CONDUCTOR_OPENAI_API_KEY` |
| `document-ingestion` | Ingest and index documents for RAG pipelines | `CONDUCTOR_OPENAI_API_KEY` |

## Running an Example

```bash
cd examples/ai/first-ai-workflow

# Simulated mode (no API key needed)
mvn package -DskipTests
java -jar target/first-ai-workflow-1.0.0.jar

# Live mode (real OpenAI calls)
CONDUCTOR_OPENAI_API_KEY=sk-... java -jar target/first-ai-workflow-1.0.0.jar
```

All examples print `Result: PASSED` on success, whether in simulated or live mode.
