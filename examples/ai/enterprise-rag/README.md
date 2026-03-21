# Enterprise RAG in Java Using Conductor :  Caching, Rate Limiting, Token Budgets, and Audit Logging

A Java Conductor workflow that wraps a RAG pipeline with guardrails commonly needed in enterprise RAG pipelines. semantic caching to avoid redundant LLM calls, per-user rate limiting to control costs, token budget enforcement to prevent context window overflows, and audit-pattern logging for every query. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate these concerns as independent workers,  you write the caching, rate-limiting, and generation logic, Conductor handles conditional routing, retries, durability, and observability.

## Beyond the Demo: What Production RAG Actually Requires

A basic RAG pipeline (retrieve, generate) works fine in a demo. In production, it falls apart. Without caching, identical questions hit the LLM repeatedly. burning tokens and adding latency. Without rate limiting, a single user (or a bug in a client) can exhaust your API budget in minutes. Without token budgets, a query that retrieves too much context overflows the model's context window and fails. And without audit logging, you can't answer "what did user X ask, and what did we tell them?",  a compliance requirement in regulated industries.

These cross-cutting concerns create branching logic: if the cache hits, skip retrieval and generation entirely but still log the query. If the rate limit is exceeded, reject the request before doing any work. If the token budget is tight, trim the retrieved context before sending it to the LLM. Each concern interacts with the others, and all of them need independent error handling.

Without orchestration, you'd layer these checks into a single method with nested conditionals, manual cache lookups, and scattered logging. code that's nearly impossible to test, debug, or modify when compliance requirements change.

## The Solution

**You write the caching, rate-limiting, token budgeting, and audit logging logic. Conductor handles the conditional routing, retries, and observability.**

Each concern is an independent worker. cache check, rate limiting, retrieval, token budget enforcement, generation, cache storage, audit logging. Conductor's `SWITCH` task skips the entire generation path on cache hits. If the rate limiter rejects a request, the pipeline stops cleanly. Every query is audit-logged regardless of which path it took, and every execution is tracked with full inputs and outputs for compliance review.

### What You Write: Workers

Seven workers implement enterprise guardrails around a RAG core. cache check, rate limiting, retrieval, token budget enforcement, generation, cache storage, and audit logging,  with a SWITCH that skips generation entirely on cache hits.

| Worker | Task | What It Does |
|---|---|---|
| **AuditLogWorker** | `er_audit_log` | Worker that creates a audit-ready pattern audit log entry for every query. Takes userId, question, sessionId, source, answ... |
| **CacheResultWorker** | `er_cache_result` | Worker that caches a generated answer for future lookups. Takes question, answer, and ttlSeconds. Returns cached stat... |
| **CheckCacheWorker** | `er_check_cache` | Worker that checks a cache for a previously answered question. Takes question and userId, returns cacheStatus and cac... |
| **GenerateWorker** | `er_generate` | Worker that generates an answer using an LLM given question, context, and token budget. Returns the answer, tokensUse... |
| **RateLimitWorker** | `er_rate_limit` | Worker that checks rate limits for a user. Takes userId, returns allowed status and rate limit details. |
| **RetrieveWorker** | `er_retrieve` | Worker that retrieves relevant context documents for a question. Returns 4 context documents with id, text, and token... |
| **TokenBudgetWorker** | `er_token_budget` | Worker that manages token budgets by trimming context to stay within limits. Takes context and userId, returns trimme... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
er_check_cache
    │
    ▼
SWITCH (cache_decision_ref)
    ├── hit: er_audit_log
    └── default: er_rate_limit -> er_retrieve -> er_token_budget -> er_generate -> er_cache_result -> er_audit_log

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/enterprise-rag-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, the generate worker calls OpenAI (gpt-4o-mini). When unset, all workers use simulated output. |

### Live vs Simulated Mode

- **Without `CONDUCTOR_OPENAI_API_KEY`**: All workers return deterministic simulated output (default behavior, no API calls).
- **With `CONDUCTOR_OPENAI_API_KEY`**: GenerateWorker calls OpenAI Chat Completions (gpt-4o-mini). RetrieveWorker remains simulated because it requires a real vector database.
- If an OpenAI call fails, the worker automatically falls back to simulated output.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/enterprise-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow enterprise_rag \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "userId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w enterprise_rag -s COMPLETED -c 5

```

## How to Extend

Each worker handles one enterprise concern. swap in Redis for caching and rate limiting, tiktoken for token budgeting, your SIEM for audit logging, and the guarded RAG pipeline runs unchanged.

- **CheckCacheWorker** / **CacheResultWorker**. swap in Redis, Memcached, or a semantic cache like GPTCache for real cache lookups and storage
- **RateLimitWorker** (`er_rate_limit`): integrate with Redis-based rate limiting (sliding window) or an API gateway's rate limit API
- **TokenBudgetWorker** (`er_token_budget`): replace with real token counting via tiktoken and budget tracking in a database
- **GenerateWorker** (`er_generate`): swap in a real LLM call to OpenAI, Claude, or a self-hosted model
- **AuditLogWorker** (`er_audit_log`): write to your SIEM, CloudWatch Logs, or a compliance audit database

Each guardrail worker maintains its pass/fail contract, so swapping Redis for Memcached or changing the audit backend requires no changes to the pipeline routing.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
enterprise-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/enterpriserag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EnterpriseRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditLogWorker.java
│       ├── CacheResultWorker.java
│       ├── CheckCacheWorker.java
│       ├── GenerateWorker.java
│       ├── RateLimitWorker.java
│       ├── RetrieveWorker.java
│       └── TokenBudgetWorker.java
└── src/test/java/enterpriserag/workers/
    ├── AuditLogWorkerTest.java        # 3 tests
    ├── CacheResultWorkerTest.java        # 3 tests
    ├── CheckCacheWorkerTest.java        # 3 tests
    ├── GenerateWorkerTest.java        # 3 tests
    ├── RateLimitWorkerTest.java        # 3 tests
    ├── RetrieveWorkerTest.java        # 3 tests
    └── TokenBudgetWorkerTest.java        # 3 tests

```
