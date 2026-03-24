# Multi-Agent Code Review in Java Using Conductor : Security, Performance, and Style Review in Parallel

Multi-Agent Code Review. parses code, runs security/performance/style reviews in parallel, then compiles a final review report.

## Code Review Needs Multiple Specialized Perspectives

A single code reviewer might catch a SQL injection vulnerability but miss an N+1 query performance issue. Or they might fix the N+1 query but overlook inconsistent naming conventions. Each review dimension. security (injection, XSS, auth bypass), performance (algorithmic complexity, database query patterns, memory allocation), and style (naming conventions, code organization, documentation), requires different expertise and different analytical approaches.

Running three specialized reviewers in parallel produces a comprehensive review in the time of the slowest reviewer, not the sum of all three. Each reviewer operates independently. they don't need to see each other's findings to do their job. The compilation step merges findings, removes duplicates, and prioritizes by severity across all three dimensions.

## The Solution

**You write the security, performance, and style review logic. Conductor handles parallel execution, finding aggregation, and severity-based prioritization.**

`ParseCodeWorker` analyzes the submitted code to extract its language, structure, imports, and AST-level metadata for the reviewers. `FORK_JOIN` dispatches three reviewers in parallel: `SecurityReviewWorker` checks for injection vulnerabilities, auth issues, and data exposure. `PerformanceReviewWorker` identifies algorithmic inefficiencies, N+1 queries, and memory issues. `StyleReviewWorker` flags naming violations, documentation gaps, and structural issues. After `JOIN` collects all findings, `CompileReviewWorker` merges them into a prioritized report organized by severity and category. Conductor runs all three reviews simultaneously and tracks how long each reviewer takes.

### What You Write: Workers

Five workers run the code review. Parsing the code, then dispatching security, performance, and style reviewers in parallel before compiling a prioritized report.

| Worker | Task | What It Does |
|---|---|---|
| **CompileReviewWorker** | `cr_compile_review` | Compile review agent. aggregates findings from security, performance, and style review agents, counts total and high.. |
| **ParseCodeWorker** | `cr_parse_code` | Parses submitted code and produces a simplified AST representation including functions, imports, line count, and comp... |
| **PerformanceReviewWorker** | `cr_performance_review` | Performance review agent. inspects the AST for performance issues such as N+1 queries and missing connection pooling. |
| **SecurityReviewWorker** | `cr_security_review` | Security review agent. inspects the AST for security vulnerabilities such as SQL injection, weak cryptography, and m.. |
| **StyleReviewWorker** | `cr_style_review` | Style review agent. inspects the AST for code style issues such as inconsistent naming, missing documentation, and o.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cr_parse_code
 │
 ▼
FORK_JOIN
 ├── cr_security_review
 ├── cr_performance_review
 └── cr_style_review
 │
 ▼
JOIN (wait for all branches)
cr_compile_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
