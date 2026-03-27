# When to Use Conductor: Problem Patterns and Example Mappings

If you're evaluating whether Conductor fits your problem, start here. Each pattern describes a real pain point and links to examples that solve it.

## I need to run steps in parallel and wait for all of them

**The problem:** You have 3-10 independent API calls, database queries, or processing steps. Running them sequentially takes too long. Running them in threads means managing thread pools, timeouts, error collection, and partial failure recovery yourself.

**Conductor pattern:** `FORK_JOIN` dispatches branches in parallel and waits for all to complete. If one branch fails, the others still run and you get partial results.

**Examples:**
- [task-patterns/fork-join](task-patterns/fork-join/) — Fetch product, inventory, and reviews in parallel, merge results
- [ecommerce/fraud-detection](ecommerce/fraud-detection/) — Run rule checks, ML scoring, and velocity analysis simultaneously
- [agents/tool-use-parallel](agents/tool-use-parallel/) — Call weather, stocks, and news APIs concurrently

## I need distributed transactions with rollback

**The problem:** Your order flow charges a credit card, reserves inventory, and books shipping. If shipping fails, you need to refund the payment AND release the inventory — in the right order. Building this with try/catch across microservices is a nightmare.

**Conductor pattern:** The Saga pattern pairs every forward step with a compensating action. On failure, Conductor runs compensations in reverse order automatically.

**Examples:**
- [resilience/saga-pattern](resilience/saga-pattern/) — Flight + hotel + payment with reverse-order compensation
- [resilience/compensation-workflows](resilience/compensation-workflows/) — Generic compensation with action logging
- [ecommerce/order-management](ecommerce/order-management/) — 5-state order lifecycle with cancellation paths

## I need a human to approve something mid-workflow

**The problem:** Your automated pipeline needs a manager to approve an expense, a doctor to review a diagnosis, or a legal team to sign off on a contract. The workflow should pause durably (survive server restarts) and resume when the human acts.

**Conductor pattern:** `WAIT` tasks pause the workflow until an external signal arrives. `HUMAN` tasks integrate with approval UIs.

**Examples:**
- [human-in-loop/four-eyes-approval](human-in-loop/four-eyes-approval/) — Dual approval before proceeding
- [human-in-loop/escalation-chain](human-in-loop/escalation-chain/) — Analyst → Manager → VP with timeouts
- [human-in-loop/expense-approval](human-in-loop/expense-approval/) — Policy validation + manager review

## I need to retry failed steps with backoff

**The problem:** Your API call to a payment gateway returns a 503. You need to retry with exponential backoff, but after 5 failures you need to route to a fallback or dead-letter queue — not just crash.

**Conductor pattern:** Task definitions configure `retryCount`, `retryDelaySeconds`, and `backoffScaleFactor`. Workers just return FAILED; Conductor handles the retry timing.

**Examples:**
- [resilience/circuit-breaker](resilience/circuit-breaker/) — Track failure rate, open circuit, serve fallback
- [resilience/dead-letter](resilience/dead-letter/) — Route failed messages for inspection
- [resilience/graceful-degradation](resilience/graceful-degradation/) — Fallback to cached data when a dependency is down

## I need to fan out N tasks where N is only known at runtime

**The problem:** A user uploads 3 images today, 50 tomorrow. You need to process each one in parallel, but you can't hardcode the number of branches in your workflow definition.

**Conductor pattern:** `FORK_JOIN_DYNAMIC` creates branches at runtime based on a prepare task's output.

**Examples:**
- [task-patterns/dynamic-fork](task-patterns/dynamic-fork/) — Process a variable number of URLs in parallel
- [task-patterns/fan-out-fan-in](task-patterns/fan-out-fan-in/) — Scatter-gather image processing

## I need to run a workflow on a schedule

**The problem:** Your nightly ETL job, weekly report, or hourly health check needs to run reliably on a cron schedule — and you need to see if it succeeded, how long it took, and what happened when it failed.

**Conductor pattern:** Schedule workflows with cron expressions. Each execution is a tracked workflow instance with full observability.

**Examples:**
- [scheduling/cron-job-orchestration](scheduling/cron-job-orchestration/) — Scheduled recurring jobs with cleanup
- [scheduling/batch-scheduling](scheduling/batch-scheduling/) — Coordinate multiple batch jobs with dependencies
- [scheduling/deadline-management](scheduling/deadline-management/) — Track deadlines, send reminders, escalate

## I need to process events from Kafka/SQS and track what happened

**The problem:** Events arrive from a message broker. You need to process each one through multiple steps, handle failures without losing the event, and track which events succeeded or failed.

**Conductor pattern:** Event-driven workflows trigger on external events. Each event becomes a tracked workflow execution.

**Examples:**
- [events/cdc-pipeline](events/cdc-pipeline/) — Capture database changes, transform, publish downstream
- [events/event-driven-saga](events/event-driven-saga/) — Saga orchestrated by events
- [events/event-routing](events/event-routing/) — Route events to handlers based on content

## I need to build an AI/LLM pipeline with retries and observability

**The problem:** Your LLM feature works in a notebook but fails in production. The API times out, the response format varies, you can't tell which step is slow, and there's no audit trail of what the model said.

**Conductor pattern:** Each LLM call is a worker with its own timeout, retry, and logging. Conductor tracks every prompt and response.

**Examples:**
- [ai/basic-rag](ai/basic-rag/) — Embed, search, generate with TF-IDF and OpenAI
- [ai/adaptive-rag](ai/adaptive-rag/) — Route queries to different retrieval strategies
- [ai/llm-fallback-chain](ai/llm-fallback-chain/) — Try GPT-4, fall back to Claude, fall back to Gemini
- [agents/function-calling](agents/function-calling/) — LLM selects and executes functions

## I need to coordinate microservices without tight coupling

**The problem:** Service A calls Service B calls Service C. When C is down, A hangs. When B changes its API, A breaks. There's no central view of the flow and debugging means correlating logs across services.

**Conductor pattern:** Each service becomes a worker that polls for tasks independently. Conductor coordinates the sequence, handles failures, and provides a single execution view.

**Examples:**
- [microservices/api-gateway](microservices/api-gateway/) — Auth, route, transform in sequence
- [microservices/choreography-vs-orchestration](microservices/choreography-vs-orchestration/) — Compare both approaches
- [devops/ci-cd-pipeline](devops/ci-cd-pipeline/) — Build, test, scan, deploy as independent workers

## I need a CI/CD pipeline with security gates

**The problem:** Your deploy pipeline needs to: build, run tests, scan for secrets, check dependencies for CVEs, deploy to staging, run integration tests, then promote to production — and block the deploy if any security check fails.

**Conductor pattern:** Each stage is a worker. `FORK_JOIN` runs tests in parallel. A SWITCH task gates production deployment on security results.

**Examples:**
- [devops/ci-cd-pipeline](devops/ci-cd-pipeline/) — Full pipeline with ProcessBuilder, security scanning, deploy validation
- [security/vulnerability-scanning](security/vulnerability-scanning/) — CVE scanning with release gating
- [devops/certificate-rotation](devops/certificate-rotation/) — SSL certificate lifecycle management

## I need to handle compliance workflows (GDPR, HIPAA, SOC2)

**The problem:** A customer requests data deletion. You need to find their data across 5 systems, verify their identity, delete everything, and produce an audit trail that proves you did it — all within 30 days.

**Conductor pattern:** Each compliance step is a tracked worker. Conductor maintains the audit trail automatically. Timeouts enforce SLA deadlines.

**Examples:**
- [security/gdpr-compliance](security/gdpr-compliance/) — PII detection, masking, audit logging
- [healthcare/clinical-trials](healthcare/clinical-trials/) — 21 CFR Part 11 audit trail
- [finance/account-opening](finance/account-opening/) — KYC, credit check, compliance audit

## When Conductor might be overkill

- **Simple request-response APIs** — If your flow is a single synchronous call, you don't need an orchestrator.
- **Pure batch processing** — If you're processing a flat file with no branching or failure handling, a simple script may suffice.
- **Sub-millisecond latency requirements** — Conductor adds orchestration overhead (typically 10-50ms per task). For ultra-low-latency hot paths, call services directly.
- **Single-service logic** — If everything runs in one process with no external calls, you don't need distributed orchestration.
