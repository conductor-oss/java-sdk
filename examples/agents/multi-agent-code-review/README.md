# Multi-Agent Code Review in Java Using Conductor :  Security, Performance, and Style Review in Parallel

Multi-Agent Code Review. parses code, runs security/performance/style reviews in parallel, then compiles a final review report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Code Review Needs Multiple Specialized Perspectives

A single code reviewer might catch a SQL injection vulnerability but miss an N+1 query performance issue. Or they might fix the N+1 query but overlook inconsistent naming conventions. Each review dimension. security (injection, XSS, auth bypass), performance (algorithmic complexity, database query patterns, memory allocation), and style (naming conventions, code organization, documentation),  requires different expertise and different analytical approaches.

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
java -jar target/multi-agent-code-review-1.0.0.jar

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

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/multi-agent-code-review-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_agent_code_review \
  --version 1 \
  --input '{"code": "sample-code", "language": "en"}'express');\\nconst crypto = require('crypto');\\n// ... application code": "sample-const express = require('express');\\nconst crypto = require('crypto');\\n// ... application code", "language": "sample-language"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_agent_code_review -s COMPLETED -c 5

```

## How to Extend

Each reviewer specializes in one code quality dimension. Integrate Semgrep/CodeQL for security, SpotBugs/PMD for performance, and Checkstyle/ESLint for style, and the parse-review-compile workflow runs unchanged.

- **SecurityReviewWorker** (`cr_security_review`): integrate with Semgrep or CodeQL for AST-based vulnerability detection, OWASP dependency-check for known CVEs in libraries, or Snyk for container security
- **PerformanceReviewWorker** (`cr_performance_review`): use static analysis tools (SpotBugs, PMD) for algorithmic complexity detection, or LLM-based analysis with performance anti-pattern catalogs
- **StyleReviewWorker** (`cr_style_review`): integrate with Checkstyle for Java conventions, ESLint/Prettier for JavaScript, or use an LLM with team-specific style guide context for custom rule enforcement

Plug in real static analysis tools like SonarQube or Semgrep; the parallel review pipeline preserves the same finding-aggregation interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
multi-agent-code-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiagentcodereview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiAgentCodeReviewExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompileReviewWorker.java
│       ├── ParseCodeWorker.java
│       ├── PerformanceReviewWorker.java
│       ├── SecurityReviewWorker.java
│       └── StyleReviewWorker.java
└── src/test/java/multiagentcodereview/workers/
    ├── CompileReviewWorkerTest.java        # 8 tests
    ├── ParseCodeWorkerTest.java        # 8 tests
    ├── PerformanceReviewWorkerTest.java        # 7 tests
    ├── SecurityReviewWorkerTest.java        # 7 tests
    └── StyleReviewWorkerTest.java        # 8 tests

```
