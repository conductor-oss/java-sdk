# Multi-Agent Customer Support in Java Using Conductor: Classify, Route by Category, Propose Solutions, QA Validate

Tier 1 support copies a customer's "I can't log in. error 403 after password reset" into the billing queue because the word "password" wasn't in the tech-support routing rules. Billing says "not my problem." Forty-eight hours later, the customer churns, over a bug that had a known fix in the knowledge base. When every ticket funnels through the same generic agent, bugs get treated like feature requests and feature requests get lost in the general queue. This example uses [Conductor](https://github.com/conductor-oss/conductor) to classify tickets, route them through a `SWITCH` to category-specific handlers (bug pipeline with KB search, feature evaluation against the roadmap, or general response), and run every answer through QA validation before it reaches the customer.

## Support Tickets Need Category-Specific Handling

A customer submits "Login fails with error 403 after password reset". that's a bug report that needs knowledge base search for known issues and a specific solution. Another submits "Can you add dark mode?", that's a feature request that needs evaluation against the product roadmap. A third asks "What's your refund policy?", that's a general inquiry with a straightforward answer.

Each category needs a different agent pipeline. Bugs need knowledge base search followed by solution proposal. Feature requests need roadmap evaluation and prioritization. General inquiries need direct response generation. After the category-specific handling, all responses need QA validation to ensure accuracy and tone before reaching the customer. Without orchestration, this branching logic with shared QA at the end becomes a sprawling if/else tree.

## The Solution

**You write the ticket classification, category-specific handlers, and QA validation. Conductor handles routing, path convergence, and response quality tracking.**

`ClassifyTicketWorker` analyzes the ticket subject and description to determine the category (bug, feature, general) with confidence score and extracted metadata (error codes, feature names). Conductor's `SWITCH` routes by category: bug tickets flow through `KnowledgeSearchWorker` (searching for known issues) and `SolutionProposeWorker` (generating a fix). Feature requests go to `FeatureEvaluateWorker` (assessing against the roadmap). General inquiries go to `GeneralRespondWorker`. All paths converge at `QaValidateWorker`, which checks response accuracy, tone, and completeness before delivery. Conductor tracks which category each ticket received and how long each path took.

### What You Write: Workers

Six workers handle support tickets. Classifying the category, routing to bug/feature/general handlers via SWITCH, and validating response quality before delivery.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyTicketWorker** | `cs_classify_ticket` | Classifies a support ticket by keyword matching in subject/description. Detects bug keywords (error, crash, bug, broken, fail), feature keywords (feature, request, enhance, add), or defaults to general. Returns category, severity (high/medium/low), matched keywords, and 0.94 confidence. |
| **KnowledgeSearchWorker** | `cs_knowledge_search` | Searches the knowledge base for articles matching the ticket keywords. Returns 3 relevant KB articles (Troubleshooting Common Errors, System Recovery Procedures, Known Issues and Workarounds) with relevance scores (0.95, 0.87, 0.82) and search time. |
| **SolutionProposeWorker** | `cs_solution_propose` | Proposes a solution based on knowledge base articles and ticket description. Returns a 5-step fix (clear cache, verify config, check resources, apply patch, escalate) with referenced KB article IDs and solution type ("known_fix"). |
| **FeatureEvaluateWorker** | `cs_feature_evaluate` | Evaluates a feature request against the product roadmap. Returns a response acknowledging alignment with roadmap, priority level (high), ETA (Q2 2026), and roadmap tracking ID (FR-4521). Premium tier customers get expedited evaluation. |
| **GeneralRespondWorker** | `cs_general_respond` | Handles general inquiries by providing a helpful response and suggesting relevant documentation (Getting Started Guide, FAQ, API Documentation). |
| **QaValidateWorker** | `cs_qa_validate` | Validates the quality of the support response by running 4 checks: tone appropriate, includes greeting, includes next steps, no sensitive data exposed. Returns approved status, check results, and a formatted final response. |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
cs_classify_ticket
    |
    v
SWITCH (route_by_category_ref)
    +-- bug: cs_knowledge_search -> cs_solution_propose
    +-- feature: cs_feature_evaluate
    +-- default: cs_general_respond
    |
    v
cs_qa_validate

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
java -jar target/multi-agent-support-1.0.0.jar

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

## Example Output

```
=== Multi-Agent Customer Support Demo ===

Step 1: Registering task definitions...
  Registered: cs_classify_ticket, cs_knowledge_search, cs_solution_propose, cs_feature_evaluate, cs_general_respond, cs_qa_validate

Step 2: Registering workflow 'multi_agent_customer_support'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  [cs_classify_ticket] Classifying ticket: Application crashes on login
  [cs_knowledge_search] Searching KB for: [error, crash, fail]
  [cs_solution_propose] Proposing solution for severity: high
  [cs_qa_validate] Validating response for ticket: TKT-9182

  Workflow ID: 2f4a6b8c-d0e2-4f6a-8b0c-d2e4f6a8b0c2

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {ticketId=TKT-9182, category=bug, approved=true, finalResponse=Dear Customer, your ticket TKT-9182 (category: bug) has been reviewed and validated. Our team has ensured the response meets quality standards. Please review the proposed solution and let us know if you need further assistance.}

Result: PASSED

```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/multi-agent-support-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Submit a bug report
conductor workflow start \
  --workflow multi_agent_customer_support \
  --version 1 \
  --input '{"ticketId": "TKT-9182", "subject": "Application crashes on login", "description": "The app crashes with error code 500 when attempting to log in after the latest update.", "customerTier": "premium"}'

# Submit a feature request
conductor workflow start \
  --workflow multi_agent_customer_support \
  --version 1 \
  --input '{"ticketId": "TKT-9183", "subject": "Add dark mode support", "description": "Please add a dark mode feature to reduce eye strain during evening usage.", "customerTier": "standard"}'

# Submit a general inquiry
conductor workflow start \
  --workflow multi_agent_customer_support \
  --version 1 \
  --input '{"ticketId": "TKT-9184", "subject": "How do I reset my password?", "description": "I forgot my password and need help resetting it.", "customerTier": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_agent_customer_support -s COMPLETED -c 5

```

## How to Extend

Each handler specializes in one support category. Integrate a classifier (HuggingFace, GPT-4), Zendesk for knowledge base search, Productboard for roadmap evaluation, and an LLM for QA validation, and the classify-route-solve-validate workflow runs unchanged.

- **ClassifyTicketWorker** (`cs_classify_ticket`): use a fine-tuned classifier (HuggingFace text-classification pipeline) or GPT-4 with few-shot examples from your actual ticket history for accurate multi-label category detection with confidence calibration
- **KnowledgeSearchWorker** (`cs_knowledge_search`): integrate with Zendesk Guide search API, Confluence search, or build a RAG pipeline over your internal knowledge base using Pinecone/Weaviate for semantic article retrieval
- **SolutionProposeWorker** (`cs_solution_propose`): use an LLM (GPT-4/Claude) with retrieved KB articles as context to generate step-by-step solutions tailored to the specific error, or integrate with runbook automation tools like PagerDuty Runbook Automation
- **FeatureEvaluateWorker** (`cs_feature_evaluate`): connect to Productboard or Aha! APIs for roadmap lookup, check for duplicate feature requests, and auto-assign priority based on customer tier and request frequency
- **QaValidateWorker** (`cs_qa_validate`): use an LLM to check factual accuracy against the knowledge base, tone appropriateness for the customer tier, and completeness of the response. Flag responses that mention internal systems or contain sensitive data.
- **Add a new category**: create a new worker class, add a case to the `SWITCH` in `workflow.json`, and update the classification keywords. No existing code changes needed.

Swap in Zendesk and LLM-powered responses; the classify-route-validate pipeline maintains the same QA contract.

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
multi-agent-support/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiagentsupport/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiAgentSupportExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyTicketWorker.java # Keyword-based ticket classification
│       ├── KnowledgeSearchWorker.java # KB article search with relevance scoring
│       ├── SolutionProposeWorker.java # Fix steps with KB article references
│       ├── FeatureEvaluateWorker.java # Roadmap evaluation with priority and ETA
│       ├── GeneralRespondWorker.java  # Helpful response with doc suggestions
│       └── QaValidateWorker.java    # 4-check quality validation
└── src/test/java/multiagentsupport/workers/
    ├── ClassifyTicketWorkerTest.java # 9 tests
    ├── KnowledgeSearchWorkerTest.java # 8 tests
    ├── SolutionProposeWorkerTest.java # 7 tests
    ├── FeatureEvaluateWorkerTest.java # 7 tests
    ├── GeneralRespondWorkerTest.java  # 7 tests
    └── QaValidateWorkerTest.java    # 9 tests

```
