# Lease Management in Java with Conductor :  Create, Sign, Activate, Renew, or Terminate

A Java Conductor workflow example for managing the full lease lifecycle. creating a lease agreement, collecting signatures, activating the lease, and then routing to renewal or termination based on the requested action. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to manage residential or commercial leases from creation through completion. A new lease must be drafted with terms (rent amount, duration, deposit), signed by both tenant and landlord, and then activated to begin the tenancy. At the end of the lease term, the workflow needs to handle either renewal (new terms, rent escalation) or termination (security deposit return, move-out inspection). The path taken depends on whether the tenant or landlord requests renewal or termination, and every action must be recorded for legal compliance.

Without orchestration, lease management is a tangle of manual steps. Property managers track lease statuses in spreadsheets, miss renewal deadlines, activate leases before signatures are collected, or forget to process terminations. A monolithic script that tries to handle both renewal and termination paths becomes a mess of if/else branches that nobody wants to touch when lease terms change.

## The Solution

**You just write the lease creation, signature collection, activation, and renewal or termination logic. Conductor handles notification retries, payment scheduling, and lease lifecycle audit trails.**

Each lease lifecycle step is a simple, independent worker. one creates the lease, one handles signing, one activates it, and then a SWITCH task routes to either renewal or termination based on the requested action. Conductor takes care of executing them in order, ensuring no lease is activated without a signature, routing to the correct end-of-lease path, and maintaining a complete history of every lease action. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Lease drafting, tenant notification, payment scheduling, and renewal tracking workers each handle one aspect of the lease lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CreateLeaseWorker** | `lse_create` | Generates a lease agreement with terms (rent, duration, deposit) for the tenant/property pair |
| **SignLeaseWorker** | `lse_sign` | Records lease signatures from tenant and landlord, producing a fully executed agreement |
| **ActivateLeaseWorker** | `lse_activate` | Activates the lease. starts the tenancy, enables rent collection, sets key dates |
| **RenewLeaseWorker** | `lse_renew` | Processes lease renewal with updated terms and rent escalation (renew path) |
| **TerminateLeaseWorker** | `lse_terminate` | Handles lease termination. schedules move-out inspection, initiates deposit return (terminate path) |

Workers implement property transaction steps. listing, inspection, escrow, closing,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
lse_create
    │
    ▼
lse_sign
    │
    ▼
lse_activate
    │
    ▼
SWITCH (lse_switch_ref)
    ├── renew: lse_renew
    ├── terminate: lse_terminate

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
java -jar target/lease-management-1.0.0.jar

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
java -jar target/lease-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lse_lease_management \
  --version 1 \
  --input '{"tenantId": "TEST-001", "propertyId": "TEST-001", "action": "process"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lse_lease_management -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real property management stack. your lease generation system, DocuSign for e-signatures, your accounting platform for rent activation and deposit handling, and the workflow runs identically in production.

- **CreateLeaseWorker** (`lse_create`): generate lease documents from templates using a document assembly API, pull property details from your MLS/property management system
- **SignLeaseWorker** (`lse_sign`): integrate with DocuSign or HelloSign for legally binding e-signatures
- **ActivateLeaseWorker** (`lse_activate`): update your property management platform (AppFolio, Buildium, Yardi) to mark the unit as occupied and schedule recurring rent charges
- **RenewLeaseWorker** (`lse_renew`): calculate CPI-based rent escalation, generate a renewal addendum, and send for signatures
- **TerminateLeaseWorker** (`lse_terminate`): schedule the move-out inspection in your maintenance system, calculate prorated rent and deposit deductions, and initiate the refund

Change lease terms or notification rules and the management pipeline handles them without modification.

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
lease-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/leasemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LeaseManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateLeaseWorker.java
│       ├── CreateLeaseWorker.java
│       ├── RenewLeaseWorker.java
│       ├── SignLeaseWorker.java
│       └── TerminateLeaseWorker.java
└── src/test/java/leasemanagement/workers/
    ├── ActivateLeaseWorkerTest.java        # 2 tests
    ├── CreateLeaseWorkerTest.java        # 2 tests
    ├── RenewLeaseWorkerTest.java        # 2 tests
    ├── SignLeaseWorkerTest.java        # 2 tests
    └── TerminateLeaseWorkerTest.java        # 2 tests

```
