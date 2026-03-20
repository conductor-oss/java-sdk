# Multi-Tenant Approval in Java Using Conductor :  Tenant Config Loading, Amount-Based SWITCH to Manager or Manager+Executive WAIT Chains, and Tenant-Scoped Finalization

A Java Conductor workflow example for multi-tenant SaaS approval routing .  loading each tenant's approval configuration (auto-approve limits, required approval levels), using a SWITCH to route requests to the correct WAIT chain based on the tenant's rules (manager-only for single-level tenants, manager-then-executive for enterprise tenants, auto-approve for tenants below threshold), and finalizing with tenant-scoped post-approval logic. Each tenant (startup-co, enterprise-corp, small-biz) has its own auto-approve limit and approval chain depth, so the same workflow handles all tenants without per-tenant workflow definitions. Uses [Conductor](https://github.## Different Tenants Have Different Approval Rules and Thresholds

In a multi-tenant SaaS application, each tenant has its own approval configuration. Different amount thresholds, different required approval levels, different approver roles. The workflow loads the tenant's configuration, determines the required approval level based on the amount, routes to the appropriate approval chain, then finalizes. If the config loading step uses stale data, you can re-run just that step.

## The Solution

**You just write the tenant-config-loading and tenant-scoped finalization workers. Conductor handles the per-tenant routing to the correct approval chain.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

MtaLoadConfigWorker loads each tenant's approval thresholds and required levels, and MtaFinalizeWorker applies tenant-scoped post-approval logic, the per-tenant SWITCH routing to the correct approval chain is declarative.

| Worker | Task | What It Does |
|---|---|---|
| **MtaLoadConfigWorker** | `mta_load_config` | Loads the tenant's approval configuration .  looks up the auto-approve limit and approval levels for the tenantId, compares against the request amount, and outputs the approvalLevel (none, manager, or executive) that determines the SWITCH path |
| *SWITCH* | `approval_switch` | Routes based on the tenant's approvalLevel: "manager" goes to a single manager WAIT, "executive" goes to sequential manager + executive WAITs, "none" skips directly to finalization | Built-in Conductor SWITCH .  no worker needed |
| *WAIT task(s)* | Manager / Executive | Pauses for each required approval level .  manager WAIT pauses until a manager approves via `POST /tasks/{taskId}`, executive WAIT (if required) pauses for a second executive approval | Built-in Conductor WAIT ,  no worker needed |
| **MtaFinalizeWorker** | `mta_finalize` | Finalizes the approved request with tenant context .  records which approval chain was used, the tenant and amount, and triggers tenant-specific post-approval logic |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
mta_load_config
    │
    ▼
SWITCH (approval_switch_ref)
    ├── manager: manager_approval_wait
    ├── executive: manager_approval_wait -> executive_approval_wait
    │
    ▼
mta_finalize
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
java -jar target/multi-tenant-approval-1.0.0.jar
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
java -jar target/multi-tenant-approval-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_tenant_approval_demo \
  --version 1 \
  --input '{"tenantId": "TEST-001", "amount": 100}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_tenant_approval_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one part of the tenant flow .  connect your SaaS config store for tenant rules and your per-tenant approval backend for finalization, and the multi-tenant routing workflow stays the same.

- **MtaFinalizeWorker** (`mta_finalize`): execute tenant-specific post-approval logic. Update the tenant's ledger, send branded notifications, or trigger tenant-specific downstream workflows
- **MtaLoadConfigWorker** (`mta_load_config`): load tenant configuration from a database, feature flag service like LaunchDarkly, or a tenant management API

Connect your tenant configuration database or LaunchDarkly and the per-tenant approval routing operates without any workflow definition changes.

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
multi-tenant-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multitenantapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiTenantApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MtaFinalizeWorker.java
│       └── MtaLoadConfigWorker.java
└── src/test/java/multitenantapproval/workers/
    ├── MtaFinalizeWorkerTest.java        # 6 tests
    └── MtaLoadConfigWorkerTest.java        # 13 tests
```
