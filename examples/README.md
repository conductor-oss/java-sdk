# Conductor Java SDK Examples

118 self-contained examples for the Conductor Java SDK. Each example is an independent Maven project with its own `pom.xml`, workers, workflow definition, tests, and launcher script -- clone one, build it, run it.

## Quick Start

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Pick any example and run it
cd examples/basics/hello-world
mvn package -DskipTests && java -jar target/hello-world-1.0.0.jar
```

## Prerequisites

- Java 21+
- Maven 3.8+
- Conductor server (local Docker or [Orkes Cloud](https://orkes.io))

---

## Basics

| Example | What it does |
|---------|-------------|
| [hello-world](basics/hello-world/) | One workflow, one task, one worker -- the minimum viable Conductor example |
| [creating-workers](basics/creating-workers/) | Implement and register workers that poll and execute tasks |
| [registering-workflows](basics/registering-workflows/) | Register workflow and task definitions programmatically via the SDK |
| [workflow-input-output](basics/workflow-input-output/) | Pass data between workflow input, tasks, and workflow output |
| [understanding-workflows](basics/understanding-workflows/) | Workflow concepts: tasks, states, transitions, and lifecycle |
| [sdk-setup](basics/sdk-setup/) | Verify Maven dependency, client config, and server connectivity |
| [docker-setup](basics/docker-setup/) | Verify a Docker-based Conductor setup with a single-task workflow |
| [orkes-cloud](basics/orkes-cloud/) | Connect to Orkes Cloud with key/secret authentication |
| [conductor-ui](basics/conductor-ui/) | A workflow designed for exploring the Conductor UI |
| [end-to-end-app](basics/end-to-end-app/) | Complete app: classify support tickets, route, resolve, notify |

## Task Patterns

| Example | What it does |
|---------|-------------|
| [fork-join](task-patterns/fork-join/) | Fetch product details, inventory, and reviews in parallel, then merge |
| [dynamic-fork](task-patterns/dynamic-fork/) | Fork N tasks in parallel where N is only known at runtime |
| [fan-out-fan-in](task-patterns/fan-out-fan-in/) | Scatter-gather image processing using FORK_JOIN_DYNAMIC |
| [sub-workflows](task-patterns/sub-workflows/) | Delegate payment handling to a reusable child workflow |
| [do-while](task-patterns/do-while/) | Process items in a batch one at a time until the batch is complete |
| [switch-task](task-patterns/switch-task/) | Route support tickets to different handlers based on priority |
| [wait-for-event](task-patterns/wait-for-event/) | Pause a workflow durably until an external signal arrives |
| [rate-limiting](task-patterns/rate-limiting/) | Task-level rate limiting with concurrency and frequency constraints |

## Agents

| Example | What it does |
|---------|-------------|
| [function-calling](agents/function-calling/) | Parse a query, select a function (weather/stocks/math), execute it with real APIs |
| [tool-use-basics](agents/tool-use-basics/) | Execute tools: Open-Meteo weather, Wikipedia search, math evaluator |
| [tool-use-parallel](agents/tool-use-parallel/) | Call weather, stocks, and news APIs in parallel and merge results |
| [chain-of-thought](agents/chain-of-thought/) | Three-step reasoning: analyze → calculate → verify |
| [react-agent](agents/react-agent/) | Reason-Act-Observe loop that iterates until the answer is found |
| [agentic-loop](agents/agentic-loop/) | Autonomous agent that loops with iteration and convergence control |
| [plan-execute-agent](agents/plan-execute-agent/) | Generate a plan, then execute each step sequentially |
| [agent-supervisor](agents/agent-supervisor/) | Supervisor assigns work to coder, tester, and documenter agents |
| [agent-collaboration](agents/agent-collaboration/) | Four agents chained: research → analyze → write → review |
| [agent-handoff](agents/agent-handoff/) | Route customer issues to the right specialist agent |
| [multi-agent-research](agents/multi-agent-research/) | Search papers, databases, and web in parallel, then synthesize |
| [multi-agent-support](agents/multi-agent-support/) | Tier-1 → Tier-2 → Tier-3 escalation with specialist routing |
| [api-calling-agent](agents/api-calling-agent/) | Authenticate via HMAC-JWT, then call real HTTP endpoints |
| [autonomous-agent](agents/autonomous-agent/) | Plan and execute infrastructure provisioning autonomously |
| [database-agent](agents/database-agent/) | Parse natural language to SQL, execute against in-memory tables |

## AI & LLM Workflows

> Requires API keys: `CONDUCTOR_OPENAI_API_KEY`, `CONDUCTOR_ANTHROPIC_API_KEY`, or `GOOGLE_API_KEY`

| Example | What it does |
|---------|-------------|
| [first-ai-workflow](ai/first-ai-workflow/) | Minimal AI workflow with retry and timeout handling |
| [openai-gpt4](ai/openai-gpt4/) | GPT-4 integration with structured prompts |
| [anthropic-claude](ai/anthropic-claude/) | Claude integration for long-context analysis |
| [google-gemini](ai/google-gemini/) | Gemini integration for multimodal tasks |
| [basic-rag](ai/basic-rag/) | Retrieval-augmented generation: embed → search → generate |
| [adaptive-rag](ai/adaptive-rag/) | Classify query complexity, route to simple/analytical/creative generation |
| [corrective-rag](ai/corrective-rag/) | Check retrieval relevance, fall back to web search if poor |
| [rag-citation](ai/rag-citation/) | RAG with inline source citations and confidence scores |
| [rag-hybrid-search](ai/rag-hybrid-search/) | BM25 keyword + TF-IDF cosine vector search, then merge results |
| [document-ingestion](ai/document-ingestion/) | Extract text from PDFs with Apache PDFBox, chunk, and store vectors |
| [llm-chain](ai/llm-chain/) | Chain multiple LLM calls: summarize → extract → validate |
| [llm-fallback-chain](ai/llm-fallback-chain/) | Try GPT-4, fall back to Claude, fall back to Gemini |
| [llm-cost-tracking](ai/llm-cost-tracking/) | Track tokens, latency, and cost per LLM call |
| [structured-output](ai/structured-output/) | Force LLM output into validated JSON schemas |

## Resilience

| Example | What it does |
|---------|-------------|
| [saga-pattern](resilience/saga-pattern/) | Book flight → reserve hotel → rent car, with compensating rollbacks |
| [compensation-workflows](resilience/compensation-workflows/) | Execute steps A → B → C; if C fails, compensate B then A |
| [circuit-breaker](resilience/circuit-breaker/) | Track failure rate, open circuit, reject fast, half-open probe |
| [dead-letter](resilience/dead-letter/) | Route failed messages to a dead-letter queue for inspection |
| [graceful-degradation](resilience/graceful-degradation/) | Serve cached/default data when a dependency is down |
| [workflow-recovery](resilience/workflow-recovery/) | Resume workflows from the last successful task after a crash |

## E-Commerce

| Example | What it does |
|---------|-------------|
| [checkout-flow](ecommerce/checkout-flow/) | Validate cart → reserve inventory → charge payment → confirm |
| [fraud-detection](ecommerce/fraud-detection/) | 7-factor weighted fraud scoring: velocity, amount, geo, device, BIN, time, history |
| [inventory-management](ecommerce/inventory-management/) | CAS-based inventory with AtomicInteger -- no overselling |
| [order-management](ecommerce/order-management/) | Order lifecycle: validate → pay → pick-and-pack → ship → notify |
| [payment-processing](ecommerce/payment-processing/) | Stripe SDK: authorize → capture → handle webhooks |

## DevOps

| Example | What it does |
|---------|-------------|
| [ci-cd-pipeline](devops/ci-cd-pipeline/) | Build → unit test → security scan → deploy staging → integration test |
| [certificate-rotation](devops/certificate-rotation/) | Discover certs → check expiry → rotate via SSLSocket inspection |
| [database-backup](devops/database-backup/) | pg_dump → compress → upload → verify integrity → cleanup old backups |
| [incident-response](devops/incident-response/) | Detect → triage → notify on-call → escalate → resolve → postmortem |
| [service-discovery-devops](devops/service-discovery-devops/) | Register services, update routing config, health-check endpoints |
| [uptime-monitor](devops/uptime-monitor/) | Check endpoints (DNS/HTTP/TLS), aggregate, alert via Slack/email |

## Events

| Example | What it does |
|---------|-------------|
| [cdc-pipeline](events/cdc-pipeline/) | Capture database changes, transform, publish downstream |
| [dead-letter-events](events/dead-letter-events/) | Catch malformed events and route them for manual inspection |
| [event-driven-saga](events/event-driven-saga/) | Saga orchestrated by events: order → payment → inventory → shipping |
| [event-notification](events/event-notification/) | Fan-out notifications via email, SMS, and push on payment failure |
| [event-routing](events/event-routing/) | Route events to handlers based on event type and content |

## Microservices

| Example | What it does |
|---------|-------------|
| [api-gateway](microservices/api-gateway/) | Authenticate → rate-limit → route → transform response |
| [blue-green-deploy](microservices/blue-green-deploy/) | Deploy to green, smoke test, switch traffic, keep blue as rollback |
| [bulkhead-pattern](microservices/bulkhead-pattern/) | Isolate resource pools so one failing service can't starve others |
| [canary-deployment](microservices/canary-deployment/) | Route 5% traffic to canary, analyze metrics, promote or rollback |
| [choreography-vs-orchestration](microservices/choreography-vs-orchestration/) | Run the same order flow both ways and compare |

## Human-in-the-Loop

| Example | What it does |
|---------|-------------|
| [customer-onboarding-kyc](human-in-loop/customer-onboarding-kyc/) | Automated KYC check, then human review for edge cases |
| [escalation-chain](human-in-loop/escalation-chain/) | Submit → analyst WAIT → manager WAIT → VP WAIT with timeouts |
| [expense-approval](human-in-loop/expense-approval/) | Validate expense against policy, route to manager if over threshold |
| [four-eyes-approval](human-in-loop/four-eyes-approval/) | Require two independent approvals before proceeding |
| [legal-contract-review](human-in-loop/legal-contract-review/) | AI extracts terms, human reviews and approves via WAIT task |

## Advanced

| Example | What it does |
|---------|-------------|
| [content-enricher](advanced/content-enricher/) | Take a sparse event, call APIs to enrich it, output a full record |
| [exactly-once](advanced/exactly-once/) | Idempotency keys + dedup to guarantee exactly-once processing |
| [idempotent-processing](advanced/idempotent-processing/) | Same input always produces same output, even on retry |
| [map-reduce](advanced/map-reduce/) | Split data into chunks, process in parallel, reduce to final result |
| [scatter-gather](advanced/scatter-gather/) | Query multiple vendors in parallel, pick the best response |
| [workflow-testing](advanced/workflow-testing/) | Unit test workflows with test fixtures and assertions |

## Data

| Example | What it does |
|---------|-------------|
| [batch-processing](data/batch-processing/) | Process millions of rows with checkpointing and restart |
| [csv-processing](data/csv-processing/) | Parse, validate, transform, and load CSV records |
| [data-aggregation](data/data-aggregation/) | Aggregate regional data into summary dashboards |
| [data-enrichment](data/data-enrichment/) | Enrich sparse records with external API lookups |
| [data-quality-checks](data/data-quality-checks/) | Validate completeness, format, range, and cross-field consistency |
| [etl-basics](data/etl-basics/) | Extract → transform → validate → load → confirm |

## Finance

| Example | What it does |
|---------|-------------|
| [account-opening](finance/account-opening/) | KYC → credit check → compliance → provision account |
| [claims-processing](finance/claims-processing/) | Intake → validate policy → assess damage → approve/deny → pay |
| [invoice-processing](finance/invoice-processing/) | OCR extract → parse line items → validate totals → route for approval |

## Security

| Example | What it does |
|---------|-------------|
| [gdpr-compliance](security/gdpr-compliance/) | Locate PII across systems, anonymize, verify deletion, audit log |
| [secrets-management](security/secrets-management/) | Generate secrets with SecureRandom, rotate, revoke expired ones |
| [vulnerability-scanning](security/vulnerability-scanning/) | Scan dependencies for CVEs, score severity, create fix tickets |

## Scheduling

| Example | What it does |
|---------|-------------|
| [anomaly-detection](scheduling/anomaly-detection/) | Compute baselines, detect outliers via z-score, alert on anomalies |
| [batch-scheduling](scheduling/batch-scheduling/) | Coordinate multiple batch jobs with dependency ordering |
| [cron-job-orchestration](scheduling/cron-job-orchestration/) | Schedule recurring jobs with cleanup and notification |
| [deadline-management](scheduling/deadline-management/) | Track deadlines, send reminders, escalate on breach |

## Healthcare

| Example | What it does |
|---------|-------------|
| [clinical-trials](healthcare/clinical-trials/) | Screen eligibility → randomize → track visits → report adverse events |
| [patient-intake](healthcare/patient-intake/) | Collect demographics → verify insurance → create medical record |

## IoT

| Example | What it does |
|---------|-------------|
| [asset-tracking](iot/asset-tracking/) | Track shipments via GPS, geofence alerts, ETA computation |
| [device-management](iot/device-management/) | Register → provision cert → configure → health check → firmware update |

## Integrations

> Requires external service credentials

| Example | What it does |
|---------|-------------|
| [elasticsearch-integration](integrations/elasticsearch-integration/) | Index documents, search, aggregate, manage cluster health |
| [github-integration](integrations/github-integration/) | Create repos, manage PRs, sync issues, trigger workflows on events |
| [jira-integration](integrations/jira-integration/) | Create issues, transition status, sync with workflow state |

## Other Domains

| Example | What it does |
|---------|-------------|
| [crm/lead-scoring](crm/lead-scoring/) | Score leads with a weighted algorithm: company size, engagement, budget, timeline |
| [media/video-processing](media/video-processing/) | Transcode to multiple resolutions, generate thumbnails, create manifest |
| [supply-chain/contract-lifecycle](supply-chain/contract-lifecycle/) | Draft → review → approve → execute → monitor → renew |
| [supply-chain/procurement-workflow](supply-chain/procurement-workflow/) | Request → get quotes → approve → issue PO → receive goods |
| [travel/travel-approval](travel/travel-approval/) | Estimate cost → auto-approve under threshold → manager review above |
| [travel/travel-booking](travel/travel-booking/) | Search → compare → book flight + hotel + car with saga rollback |
| [user-mgmt/password-reset](user-mgmt/password-reset/) | Generate secure token → verify expiry → PBKDF2 hash → notify |
| [user-mgmt/user-onboarding](user-mgmt/user-onboarding/) | Create account → verify email → set preferences → send welcome |

---

## Example Structure

Every example follows the same layout:

```
examples/<category>/<example>/
├── pom.xml                    # Standalone Maven build (Java 21)
├── run.sh                     # Launcher script
├── README.md                  # Full documentation
├── src/main/java/             # Main class + workers
├── src/main/resources/        # workflow.json
└── src/test/java/             # Unit tests
```

All examples support `--workers` flag for worker-only mode.

## Legacy Examples

The original Gradle-based examples are preserved in [old/](old/).
