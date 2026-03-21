# Conductor Java SDK Examples

118 curated examples demonstrating how to use the Conductor Java SDK for building distributed applications, workflow orchestration, and AI-native workflows. Each example is a standalone Maven project that can be built and run independently.

## Quick Start

```bash
# Start Conductor locally
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Run any example
cd examples/basics/hello-world
mvn package -DskipTests
java -jar target/hello-world-1.0.0.jar

# Or use the launcher script
./run.sh

```

## Examples by Category

### Basics (10 examples)

| Example | Description |
|---------|-------------|
| [hello-world](basics/hello-world/) | Minimal workflow with a single worker |
| [creating-workers](basics/creating-workers/) | How to implement and register workers |
| [registering-workflows](basics/registering-workflows/) | Programmatic workflow registration |
| [workflow-input-output](basics/workflow-input-output/) | Passing data between tasks |
| [understanding-workflows](basics/understanding-workflows/) | Workflow concepts and lifecycle |
| [sdk-setup](basics/sdk-setup/) | SDK configuration and client setup |
| [docker-setup](basics/docker-setup/) | Running Conductor with Docker |
| [orkes-cloud](basics/orkes-cloud/) | Connecting to Orkes Cloud |
| [conductor-ui](basics/conductor-ui/) | Using the Conductor UI |
| [end-to-end-app](basics/end-to-end-app/) | Complete application walkthrough |

### Agents & AI Patterns (12 examples)

| Example | Description |
|---------|-------------|
| [function-calling](agents/function-calling/) | LLM-style function selection and execution |
| [tool-use-basics](agents/tool-use-basics/) | Tool execution with Open-Meteo and Wikipedia APIs |
| [tool-use-parallel](agents/tool-use-parallel/) | Parallel tool execution (weather, stocks, news) |
| [chain-of-thought](agents/chain-of-thought/) | Step-by-step reasoning pipeline |
| [react-agent](agents/react-agent/) | Reason-Act-Observe agent loop |
| [agentic-loop](agents/agentic-loop/) | Autonomous agent with iteration control |
| [plan-execute-agent](agents/plan-execute-agent/) | Plan then execute pattern |
| [agent-supervisor](agents/agent-supervisor/) | Supervisor coordinating specialist agents |
| [agent-collaboration](agents/agent-collaboration/) | Multi-agent collaboration |
| [agent-handoff](agents/agent-handoff/) | Agent-to-agent task handoff |
| [multi-agent-research](agents/multi-agent-research/) | Parallel research across sources |
| [multi-agent-support](agents/multi-agent-support/) | Multi-tier customer support |

### AI & LLM Workflows (14 examples)

> **Note:** AI examples require API keys. Set `CONDUCTOR_OPENAI_API_KEY`, `CONDUCTOR_ANTHROPIC_API_KEY`, or `GOOGLE_API_KEY` as needed. Examples throw `IllegalStateException` if keys are missing.

| Example | Description |
|---------|-------------|
| [first-ai-workflow](ai/first-ai-workflow/) | Minimal AI workflow |
| [openai-gpt4](ai/openai-gpt4/) | OpenAI GPT-4 integration |
| [anthropic-claude](ai/anthropic-claude/) | Anthropic Claude integration |
| [google-gemini](ai/google-gemini/) | Google Gemini integration |
| [basic-rag](ai/basic-rag/) | Basic retrieval-augmented generation |
| [adaptive-rag](ai/adaptive-rag/) | Query-adaptive RAG routing |
| [corrective-rag](ai/corrective-rag/) | RAG with relevance checking |
| [rag-citation](ai/rag-citation/) | RAG with source citations |
| [rag-hybrid-search](ai/rag-hybrid-search/) | BM25 + TF-IDF cosine hybrid search |
| [document-ingestion](ai/document-ingestion/) | PDF extraction with Apache PDFBox |
| [llm-chain](ai/llm-chain/) | Chained LLM calls |
| [llm-fallback-chain](ai/llm-fallback-chain/) | LLM provider failover |
| [llm-cost-tracking](ai/llm-cost-tracking/) | Token and cost tracking |
| [structured-output](ai/structured-output/) | Structured JSON output from LLMs |

### Task Patterns (8 examples)

| Example | Description |
|---------|-------------|
| [fork-join](task-patterns/fork-join/) | Parallel execution with join |
| [dynamic-fork](task-patterns/dynamic-fork/) | Runtime-determined parallel tasks |
| [fan-out-fan-in](task-patterns/fan-out-fan-in/) | Dynamic fan-out with aggregation |
| [sub-workflows](task-patterns/sub-workflows/) | Nested workflow execution |
| [do-while](task-patterns/do-while/) | Loop until condition met |
| [switch-task](task-patterns/switch-task/) | Conditional branching |
| [wait-for-event](task-patterns/wait-for-event/) | External event triggers |
| [rate-limiting](task-patterns/rate-limiting/) | Controlled execution rates |

### Resilience (6 examples)

| Example | Description |
|---------|-------------|
| [saga-pattern](resilience/saga-pattern/) | Distributed transactions with compensation |
| [compensation-workflows](resilience/compensation-workflows/) | Compensating actions on failure |
| [circuit-breaker](resilience/circuit-breaker/) | Circuit breaker pattern |
| [dead-letter](resilience/dead-letter/) | Failed task handling |
| [graceful-degradation](resilience/graceful-degradation/) | Fallback on service failure |
| [workflow-recovery](resilience/workflow-recovery/) | Recovery from partial failures |

### E-Commerce (5 examples)

| Example | Description |
|---------|-------------|
| [checkout-flow](ecommerce/checkout-flow/) | End-to-end checkout |
| [fraud-detection](ecommerce/fraud-detection/) | 7-factor fraud scoring model |
| [inventory-management](ecommerce/inventory-management/) | CAS-based inventory with AtomicInteger |
| [order-management](ecommerce/order-management/) | Order lifecycle management |
| [payment-processing](ecommerce/payment-processing/) | Stripe SDK payment processing |

### DevOps (6 examples)

| Example | Description |
|---------|-------------|
| [ci-cd-pipeline](devops/ci-cd-pipeline/) | Build, test, deploy pipeline |
| [certificate-rotation](devops/certificate-rotation/) | SSL certificate management |
| [database-backup](devops/database-backup/) | PostgreSQL backup with pg_dump |
| [incident-response](devops/incident-response/) | Automated incident handling |
| [service-discovery-devops](devops/service-discovery-devops/) | Service registration and routing |
| [uptime-monitor](devops/uptime-monitor/) | Endpoint health monitoring |

### Microservices (5 examples)

| Example | Description |
|---------|-------------|
| [api-gateway](microservices/api-gateway/) | API gateway pattern |
| [blue-green-deploy](microservices/blue-green-deploy/) | Blue-green deployment |
| [bulkhead-pattern](microservices/bulkhead-pattern/) | Bulkhead isolation |
| [canary-deployment](microservices/canary-deployment/) | Canary release strategy |
| [choreography-vs-orchestration](microservices/choreography-vs-orchestration/) | Pattern comparison |

### Events (5 examples)

| Example | Description |
|---------|-------------|
| [cdc-pipeline](events/cdc-pipeline/) | Change data capture |
| [dead-letter-events](events/dead-letter-events/) | Dead letter event handling |
| [event-driven-saga](events/event-driven-saga/) | Event-driven saga pattern |
| [event-notification](events/event-notification/) | Event-based notifications |
| [event-routing](events/event-routing/) | Content-based event routing |

### Human-in-the-Loop (5 examples)

| Example | Description |
|---------|-------------|
| [customer-onboarding-kyc](human-in-loop/customer-onboarding-kyc/) | KYC verification workflow |
| [escalation-chain](human-in-loop/escalation-chain/) | Multi-level escalation |
| [expense-approval](human-in-loop/expense-approval/) | Expense approval workflow |
| [four-eyes-approval](human-in-loop/four-eyes-approval/) | Dual approval pattern |
| [legal-contract-review](human-in-loop/legal-contract-review/) | Contract review with WAIT tasks |

### Advanced (6 examples)

| Example | Description |
|---------|-------------|
| [content-enricher](advanced/content-enricher/) | Content enrichment pipeline |
| [exactly-once](advanced/exactly-once/) | Exactly-once processing |
| [idempotent-processing](advanced/idempotent-processing/) | Idempotent task execution |
| [map-reduce](advanced/map-reduce/) | MapReduce with Conductor |
| [scatter-gather](advanced/scatter-gather/) | Scatter-gather aggregation |
| [workflow-testing](advanced/workflow-testing/) | Workflow unit testing |

### Security (3 examples)

| Example | Description |
|---------|-------------|
| [gdpr-compliance](security/gdpr-compliance/) | GDPR data handling |
| [secrets-management](security/secrets-management/) | Secret generation and management |
| [vulnerability-scanning](security/vulnerability-scanning/) | Dependency vulnerability scanning |

### Finance (3 examples)

| Example | Description |
|---------|-------------|
| [account-opening](finance/account-opening/) | Account opening workflow |
| [claims-processing](finance/claims-processing/) | Insurance claims processing |
| [invoice-processing](finance/invoice-processing/) | Invoice OCR and validation |

### Data (6 examples)

| Example | Description |
|---------|-------------|
| [batch-processing](data/batch-processing/) | Batch data processing |
| [csv-processing](data/csv-processing/) | CSV parsing and transformation |
| [data-aggregation](data/data-aggregation/) | Data aggregation pipeline |
| [data-enrichment](data/data-enrichment/) | Data enrichment workflow |
| [data-quality-checks](data/data-quality-checks/) | Data quality validation |
| [etl-basics](data/etl-basics/) | Extract-Transform-Load pipeline |

### Scheduling (4 examples)

| Example | Description |
|---------|-------------|
| [anomaly-detection](scheduling/anomaly-detection/) | Time-series anomaly detection |
| [batch-scheduling](scheduling/batch-scheduling/) | Scheduled batch processing |
| [cron-job-orchestration](scheduling/cron-job-orchestration/) | Cron-based job scheduling |
| [deadline-management](scheduling/deadline-management/) | Deadline tracking and escalation |

### Integrations (3 examples)

> **Note:** Integration examples require external service credentials.

| Example | Description |
|---------|-------------|
| [elasticsearch-integration](integrations/elasticsearch-integration/) | Elasticsearch indexing workflow |
| [github-integration](integrations/github-integration/) | GitHub API integration |
| [jira-integration](integrations/jira-integration/) | Jira issue management |

### Other Domains

| Category | Examples | Description |
|----------|----------|-------------|
| [crm/](crm/) | lead-scoring | Weighted lead scoring algorithm |
| [healthcare/](healthcare/) | clinical-trials, patient-intake | Clinical trial management, patient onboarding |
| [iot/](iot/) | asset-tracking, device-management | IoT device lifecycle |
| [media/](media/) | video-processing | Video transcoding pipeline |
| [supply-chain/](supply-chain/) | contract-lifecycle, procurement-workflow | Supply chain management |
| [travel/](travel/) | travel-approval, travel-booking | Travel request and booking |
| [user-mgmt/](user-mgmt/) | password-reset, user-onboarding | User management workflows |

## Example Structure

Each example is a standalone Maven project:

```
examples/basics/hello-world/
├── pom.xml # Maven build (Java 21)
├── run.sh # Launcher script
├── README.md # Documentation
├── src/main/java/helloworld/
│ ├── HelloWorldExample.java # Main class
│ ├── ConductorClientHelper.java # Client helper
│ └── workers/
│ └── GreetWorker.java # Worker implementation
├── src/main/resources/
│ └── workflow.json # Workflow definition
└── src/test/java/helloworld/workers/
 └── GreetWorkerTest.java # Unit tests

```

All examples support `--workers` flag for worker-only mode (useful when starting workflows via CLI or UI).

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Conductor Server** — run locally via Docker or connect to [Orkes Cloud](https://orkes.io)

```bash
# Start Conductor locally
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

```

## Legacy Examples

The original Gradle-based examples are preserved in the [old/](old/) directory.

## Learn More

- [Conductor Java SDK Documentation](../README.md)
- [Worker SDK Guide](../java-sdk/worker_sdk.md)
- [Workflow SDK Guide](../java-sdk/workflow_sdk.md)
- [Official Conductor Documentation](https://orkes.io/content)

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
