# Wire Transfer in Java with Conductor

Wire transfer workflow: validate, verify sender, compliance check, execute, and confirm. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to execute a wire transfer between bank accounts. The workflow validates the transfer details (amount, currency, account numbers), verifies the sender's identity and account balance, runs compliance checks (sanctions screening, transaction monitoring), executes the wire through the payment network, and confirms completion. Executing a wire without compliance checks exposes the bank to regulatory penalties; insufficient balance checks result in overdrafts.

Without orchestration, you'd build a single wire service that validates, authenticates, screens, transmits via SWIFT/Fedwire, and confirms .  manually handling the two-phase nature of wire execution (reserve funds, then release), retrying failed network transmissions, and maintaining an audit trail for BSA/AML compliance.

## The Solution

**You just write the wire transfer workers. Detail validation, sender verification, sanctions screening, network execution, and confirmation. Conductor handles sequential execution, automatic retries when the payment network is temporarily unavailable, and a complete audit trail for BSA/AML compliance.**

Each wire transfer concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (validate, verify sender, compliance check, execute, confirm), retrying if the payment network is temporarily unavailable, tracking every wire with complete audit trail for regulatory compliance, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle the wire lifecycle: ValidateWorker checks transfer details and routing numbers, VerifySenderWorker authenticates the sender and confirms funds, ComplianceCheckWorker screens against sanctions lists, ExecuteWorker transmits via the payment network, and ConfirmWorker records the settlement.

| Worker | Task | What It Does |
|---|---|---|
| **ComplianceCheckWorker** | `wir_compliance_check` | Compliance Check. Computes and returns cleared, ctr required, sanctions cleared, risk score |
| **ConfirmWorker** | `wir_confirm` | Confirms the operation and computes confirmed, confirmation number, notified parties |
| **ExecuteWorker** | `wir_execute` | Execute. Computes and returns transaction ref, network, settled at, fee |
| **ValidateWorker** | `wir_validate` | Validates the input data and computes valid, routing valid, recipient bank verified, swift code |
| **VerifySenderWorker** | `wir_verify_sender` | Verifying account |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
wir_validate
    │
    ▼
wir_verify_sender
    │
    ▼
wir_compliance_check
    │
    ▼
wir_execute
    │
    ▼
wir_confirm

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
java -jar target/wire-transfer-1.0.0.jar

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
java -jar target/wire-transfer-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wire_transfer_workflow \
  --version 1 \
  --input '{"transferId": "TEST-001", "senderAccount": 10, "recipientAccount": 10, "amount": 100, "currency": "USD"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wire_transfer_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ValidateWorker to your core banking system, ComplianceCheckWorker to your OFAC/sanctions screening service, and ExecuteWorker to your SWIFT or Fedwire payment network interface. The workflow definition stays exactly the same.

- **Validator**: validate SWIFT/ABA routing numbers, IBAN format, and currency codes against ISO standards
- **Sender verifier**: authenticate the sender via your core banking system and verify sufficient funds with holds for pending wires
- **Compliance checker**: screen against OFAC/SDN lists, run transaction monitoring rules (unusual amount, high-risk countries), and flag for SAR if needed
- **Wire executor**: transmit via SWIFT (MT103), Fedwire, or ACH through your correspondent banking relationships
- **Confirmation handler**: receive and process payment acknowledgments, update account balances, and send confirmation to both parties

Swap in real SWIFT/Fedwire interfaces and OFAC screening while preserving the same output contract, and the wire transfer workflow continues without modification.

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
wire-transfer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/wiretransfer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WireTransferExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComplianceCheckWorker.java
│       ├── ConfirmWorker.java
│       ├── ExecuteWorker.java
│       ├── ValidateWorker.java
│       └── VerifySenderWorker.java
└── src/test/java/wiretransfer/workers/
    └── ComplianceCheckWorkerTest.java        # 3 tests

```
