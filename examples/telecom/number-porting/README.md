# Number Porting in Java Using Conductor

A Java Conductor workflow example that orchestrates phone number porting between carriers. submitting a port request for a phone number, validating the number's eligibility with the losing carrier, coordinating the port window between both carriers, executing the number port in the routing database, and verifying the number is reachable on the new carrier. Uses [Conductor](https://github.

## Why Number Porting Needs Orchestration

Porting a phone number between carriers requires precise coordination across independent systems. You submit a port request with the subscriber's phone number and destination carrier. You validate that the number is portable. confirming the number exists on the losing carrier's network and is not under contract lock. You coordinate with both the gaining and losing carriers to agree on a port window. You execute the actual port by updating routing tables in the Number Portability Administration Center (NPAC). Finally, you verify the number is reachable on the new carrier's network.

If the port execution succeeds but verification fails, the subscriber has a number that routes incorrectly and can't receive calls. If coordination fails after validation, you need to know exactly which carrier acknowledged the port window so you can cancel cleanly. Without orchestration, you'd build a fragile script that mixes NPAC API calls, carrier-to-carrier messaging, and routing database updates. making it impossible to retry a failed port execution without re-running the entire process from scratch.

## The Solution

**You just write the port request, eligibility validation, carrier coordination, number execution, and verification logic. Conductor handles carrier coordination retries, assignment sequencing, and porting audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Port request validation, carrier coordination, number assignment, and activation workers each manage one phase of transferring a phone number between carriers.

| Worker | Task | What It Does |
|---|---|---|
| **CoordinateWorker** | `npt_coordinate` | Coordinates the port window between the gaining and losing carriers using the port ID. |
| **PortWorker** | `npt_port` | Executes the number port by updating routing tables so the phone number routes to the new carrier. |
| **RequestWorker** | `npt_request` | Submits the port request for a phone number to the destination carrier and returns a port ID. |
| **ValidateWorker** | `npt_validate` | Validates the phone number's portability with the losing carrier. checking eligibility and contract status. |
| **VerifyWorker** | `npt_verify` | Verifies the ported number is reachable on the new carrier's network after the port completes. |

Workers implement telecom operations. provisioning, activation, billing,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
npt_request
    │
    ▼
npt_validate
    │
    ▼
npt_coordinate
    │
    ▼
npt_port
    │
    ▼
npt_verify

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
java -jar target/number-porting-1.0.0.jar

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
java -jar target/number-porting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow npt_number_porting \
  --version 1 \
  --input '{"phoneNumber": "sample-phoneNumber", "fromCarrier": "sample-fromCarrier", "toCarrier": "sample-toCarrier"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w npt_number_porting -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real porting systems. the NPAC API for eligibility checks, your inter-carrier messaging platform for port coordination, your routing database for number activation, and the workflow runs identically in production.

- **RequestWorker** (`npt_request`): submit the port request to your local number portability clearinghouse (e.g., NPAC via SOA/LSMS API, or the equivalent national porting system)
- **ValidateWorker** (`npt_validate`): query the losing carrier's Customer Service Record (CSR) via LSR/ASR interfaces to confirm the number is eligible for porting
- **CoordinateWorker** (`npt_coordinate`): exchange port coordination messages between carriers via the NPAC SOA interface or carrier-to-carrier APIs, agreeing on the due date and port window
- **PortWorker** (`npt_port`): execute the port by sending the subscription version update to the NPAC, which propagates the routing change to all carriers' LSMS databases
- **VerifyWorker** (`npt_verify`): place a test call or query the new carrier's HLR/HSS to confirm the number is routing correctly on the gaining network

Swap carrier integration APIs or validation systems and the porting pipeline adapts seamlessly.

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
number-porting-number-porting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/numberporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NumberPortingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CoordinateWorker.java
│       ├── PortWorker.java
│       ├── RequestWorker.java
│       ├── ValidateWorker.java
│       └── VerifyWorker.java
└── src/test/java/numberporting/workers/
    ├── PortWorkerTest.java        # 1 tests
    └── RequestWorkerTest.java        # 1 tests

```
