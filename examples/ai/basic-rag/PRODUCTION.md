# Basic RAG — Production Deployment Guide

## Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONDUCTOR_BASE_URL` | Yes | Conductor server API endpoint (e.g., `https://orkes.example.com/api`) |
| `CONDUCTOR_OPENAI_API_KEY` | Yes | OpenAI API key for embeddings and chat completions |
| `OPENAI_EMBED_MODEL` | No | Embedding model override (default: `text-embedding-3-small`) |
| `OPENAI_CHAT_MODEL` | No | Chat model override (default: `gpt-4o-mini`) |

All required variables must be set before starting workers. Missing keys cause `IllegalStateException` at startup or execution time — no fallback behavior exists.

## LLM Provider Setup

1. Create an OpenAI API key at https://platform.openai.com/api-keys
2. Enable billing and set usage limits to prevent runaway costs
3. For production, use a dedicated organization and project-scoped key
4. Recommended models:
   - Embeddings: `text-embedding-3-small` (fast, cost-effective) or `text-embedding-3-large` (higher quality)
   - Chat: `gpt-4o-mini` (balanced) or `gpt-4o` (highest quality)

## Security Considerations

- Store `CONDUCTOR_OPENAI_API_KEY` in a secrets manager (Vault, AWS Secrets Manager, GCP Secret Manager)
- Never commit API keys to source control
- Use network-level restrictions (VPC, firewall rules) to limit outbound traffic to `api.openai.com`
- Rotate API keys on a 90-day schedule
- Enable OpenAI audit logging for compliance

## Error Handling

Workers classify errors as:
- **Retryable** (`FAILED`): HTTP 429 (rate limit), HTTP 503 (overloaded) — Conductor will retry based on task definition `retryCount`
- **Terminal** (`FAILED_WITH_TERMINAL_ERROR`): HTTP 400/401/403, invalid inputs — no retry, requires manual intervention

## Deployment

```bash
# Build
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests

# Run
java -jar target/basic-rag-1.0.0.jar
```

For containerized deployment, use the provided `Dockerfile` and `docker-compose.yml`.

## Monitoring

- Monitor workflow failure rates in the Conductor UI
- Set alerts on:
  - `brag_embed_query` task failures (indicates OpenAI API issues)
  - `brag_search_vectors` task failures with "No documents matched" (indicates query/corpus mismatch)
  - `brag_generate_answer` task failures (indicates LLM or context issues)
- Track `tokensUsed` output from GenerateAnswerWorker for cost monitoring
- Log aggregation: workers write to stderr on failures, stdout for normal operation

## Scaling

- Increase `retryCount` in task definitions for production (recommended: 3 with exponential backoff)
- For high-throughput: deploy multiple worker instances and increase `threadCount` in `TaskRunnerConfigurer`
- Consider OpenAI rate limits: Tier 1 allows 500 RPM for embeddings, 500 RPM for chat
