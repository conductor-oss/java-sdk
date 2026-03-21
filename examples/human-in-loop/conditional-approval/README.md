# Conditional Approval Routing in Java Using Conductor :  Amount-Based Tier Classification and Multi-Level Approval Chains via SWITCH

Conditional approval routing .  classifies a request amount into a tier (low/medium/high), then uses SWITCH to route to different approval chains: low-amount requests need only manager approval, medium requests need manager + director, and high-amount requests need manager + director + VP. Each approval level is a WAIT task pausing for that approver's human decision. Uses [Conductor](https://github.

## The Problem

You need approval chains that vary based on the request amount. A $500 office supply purchase needs only a manager's sign-off. A $5,000 conference budget needs the manager and director. A $50,000 vendor contract needs manager, director, and VP. The system must classify the amount into a tier (low: under $1,000, medium: $1,000-$9,999, high: $10,000+), then route to the correct approval chain .  each level pausing for a human decision before advancing to the next. If any approver rejects, the chain stops. Hardcoding these rules in if/else blocks makes them impossible to change without a code deploy, and there is no visibility into which tier a request was classified as or where it is in the approval chain.

Without orchestration, you'd build a monolithic approval system with nested conditionals .  check the amount, send the first email, poll for a response, if approved check if more levels are needed, send the next email, and so on. If the system crashes between the manager's approval and the director's notification, the request is stuck with no one knowing it needs attention. Finance auditors need to see the exact approval chain each request went through, who approved at each level, and how long each level took.

## The Solution

**You just write the tier-classification and request-processing workers. Conductor handles the amount-based routing to the correct approval chain.**

The SWITCH task is the key pattern here. After the classify worker determines the tier, Conductor's SWITCH routes to the correct approval chain .  low goes to a single WAIT, medium goes to two sequential WAITs, high goes to three sequential WAITs. Each WAIT pauses for a human approver at that level (manager, director, VP). After all approvals in the selected chain are complete, the process worker finalizes the request. Conductor takes care of routing to the correct chain based on tier, waiting at each approval level, tracking who approved and when at every level, and providing a complete audit trail showing the tier classification and every approval in the chain. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ClassifyWorker determines the spending tier, and ProcessWorker finalizes after all approvals complete. Neither manages the SWITCH routing or the chain of WAIT tasks for manager, director, and VP.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `car_classify` | Classifies the request amount into a tier .  low (under $1,000, manager only), medium ($1,000-$9,999, manager + director), or high ($10,000+, manager + director + VP) |
| *SWITCH task* | `route_approval` | Routes to the correct approval chain based on the tier .  low, medium, or high each maps to a different sequence of WAIT tasks | Built-in Conductor SWITCH ,  no worker needed |
| *WAIT tasks* | `mgr_*/dir_*/vp_*` | Each approval level pauses for the designated approver's decision .  manager, director, or VP depending on the tier and level | Built-in Conductor WAIT ,  no worker needed |
| **ProcessWorker** | `car_process` | Finalizes the request after all required approvals are complete, recording the tier classification and approval chain results |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
car_classify
    │
    ▼
SWITCH (route_ref)
    ├── low: mgr_only_approval
    ├── medium: mgr_med_approval -> dir_med_approval
    ├── high: mgr_hi_approval -> dir_hi_approval -> vp_hi_approval
    │
    ▼
car_process

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
java -jar target/conditional-approval-1.0.0.jar

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
java -jar target/conditional-approval-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow conditional_approval_demo \
  --version 1 \
  --input '{"requestId": "TEST-001", "amount": 100, "requester": "sample-requester"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w conditional_approval_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one step of the conditional flow .  plug in your approval policy engine for tier thresholds and your procurement system (Coupa, SAP Ariba) for finalization, and the multi-level routing workflow stays the same.

- **ClassifyWorker** → pull tier thresholds from a configuration service so finance can adjust limits ($1K/$10K/$50K) without code changes, and add department-specific overrides
- **ProcessWorker** → update the request status in your procurement system, trigger purchase orders, and send approval confirmation to the requester
- **WAIT tasks** → complete each approval level from your approval UI by calling `POST /tasks/{taskId}` with `{ "approved": true }` or `{ "approved": false, "reason": "..." }`
- Add a **NotifyWorker** between each WAIT to email the next approver in the chain that a request is awaiting their review
- Add timeout on each WAIT to auto-escalate .  manager approvals escalate after 24 hours, director after 48 hours, VP after 72 hours
- Add a "reject" SWITCH case at each level to short-circuit the chain and notify the requester immediately without continuing to higher levels

Replace the simulated tier classifier with your configurable policy engine and the amount-based approval routing remains intact.

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
conditional-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/conditionalapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ConditionalApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       └── ProcessWorker.java
└── src/test/java/conditionalapproval/workers/
    ├── ClassifyWorkerTest.java        # 11 tests
    └── ProcessWorkerTest.java        # 6 tests

```
