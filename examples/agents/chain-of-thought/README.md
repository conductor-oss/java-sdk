# Chain-of-Thought in Java Using Conductor: Understand, Reason, Calculate, Verify, Answer

You ask the model "What's the compound interest on $10,000 at 5% for 3 years?" and it confidently replies "$11,576.25." Wrong. The real answer is $11,576.25 only if compounded annually; but you didn't specify, and the model didn't ask. Worse, you can't tell where it went wrong because the entire reasoning happened inside a single opaque API call. Did it misunderstand the problem, pick the wrong formula, or botch the arithmetic? This example externalizes each reasoning step: understand, reason, calculate, verify, answer, as a separate Conductor worker, so you can see exactly which step failed and retry just that step. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Making AI Reasoning Transparent and Debuggable

When an LLM answers a complex question in a single call, you can't see where it went wrong. Did it misunderstand the problem? Was its approach correct but the calculation wrong? Did it skip the verification step? Chain-of-thought prompting helps, but the reasoning is still a single opaque text blob.

Externalizing each reasoning step as a separate worker makes the thought process observable. You can see that the model understood the problem correctly (step 1), chose a valid approach (step 2), but made an arithmetic error (step 3) that the verification step caught (step 4). If the calculation step fails or produces an incorrect result, you can retry just that step with the understanding and reasoning context preserved.

## The Solution

**You write the understanding, reasoning, calculation, and verification logic. Conductor handles the reasoning chain, step-level retries, and full observability into each thought.**

`UnderstandProblemWorker` parses the problem statement and extracts the key question, known values, and what needs to be found. `Step1ReasonWorker` develops the solution approach, which formulas to use, what assumptions to make, and the logical sequence. `Step2CalculateWorker` executes the calculations step by step using the chosen approach. `Step3VerifyWorker` checks the result by substituting back into the original constraints or using an alternative method. `FinalAnswerWorker` assembles the verified result with the full reasoning chain as the final answer. Conductor makes each reasoning step individually observable and retriable.

### What You Write: Workers

Five workers externalize the reasoning chain. Understanding the problem, choosing an approach, calculating step by step, verifying the result, and assembling the final answer.

| Worker | Task | What It Does |
|---|---|---|
| **FinalAnswerWorker** | `ct_final_answer` | Produces the final human-readable answer combining the verified result and confidence score. |
| **Step1ReasonWorker** | `ct_step_1_reason` | Takes the problem understanding and produces a reasoning step identifying the formula and variables to use. |
| **Step2CalculateWorker** | `ct_step_2_calculate` | Step2s Calculate and computes calculation, interest |
| **Step3VerifyWorker** | `ct_step_3_verify` | Verifies the calculation result using fixed (hardcoded) verification values. No year-by-year computation is performed. |
| **UnderstandProblemWorker** | `ct_understand_problem` | Analyzes the incoming problem and produces a structured understanding including the problem type and known values. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
ct_understand_problem
    │
    ▼
ct_step_1_reason
    │
    ▼
ct_step_2_calculate
    │
    ▼
ct_step_3_verify
    │
    ▼
ct_final_answer

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
java -jar target/chain-of-thought-1.0.0.jar

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
java -jar target/chain-of-thought-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow chain_of_thought \
  --version 1 \
  --input '{"problem": "What is the compound interest on $10,000 at 5% annual rate for 3 years?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w chain_of_thought -s COMPLETED -c 5

```

## How to Extend

Each worker externalizes one reasoning step. Replace simulations with an LLM for problem understanding, a computation engine (Wolfram Alpha, BigDecimal) for calculation, and formal verification for result checking, and the understand-reason-calculate-verify pipeline runs unchanged.

- **Step2CalculateWorker** (`ct_step_2_calculate`): use a dedicated computation engine (Wolfram Alpha API, SymPy, or Java's BigDecimal) for reliable arithmetic instead of LLM-based calculation
- **Step3VerifyWorker** (`ct_step_3_verify`): implement formal verification: substitute the answer back into the original equations, cross-check with known bounds, or use a second LLM call as an independent verifier
- **UnderstandProblemWorker** (`ct_understand_problem`): use GPT-4 with structured output to extract problem components (given values, unknowns, constraints, relationships) into a machine-readable format

Plug in real LLM reasoning for each step; the reasoning chain uses the same understand-reason-calculate-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
chain-of-thought/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/chainofthought/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChainOfThoughtExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalAnswerWorker.java
│       ├── Step1ReasonWorker.java
│       ├── Step2CalculateWorker.java
│       ├── Step3VerifyWorker.java
│       └── UnderstandProblemWorker.java
└── src/test/java/chainofthought/workers/
    ├── FinalAnswerWorkerTest.java        # 8 tests
    ├── Step1ReasonWorkerTest.java        # 8 tests
    ├── Step2CalculateWorkerTest.java        # 8 tests
    ├── Step3VerifyWorkerTest.java        # 8 tests
    └── UnderstandProblemWorkerTest.java        # 8 tests

```
