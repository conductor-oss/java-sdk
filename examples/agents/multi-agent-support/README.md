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

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
