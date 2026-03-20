# Telecom Provisioning in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom service provisioning .  creating a service order for a customer, validating the order against the selected plan, configuring network resources for the service type, activating the service on the network, and sending a confirmation to the customer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Service Provisioning Needs Orchestration

Provisioning a new telecom service requires a strict sequence where each step depends on the previous one. You create a service order with the customer's details and service type. You validate that the order is compatible with the selected plan. You configure the network equipment (switches, routers, HLR/HSS entries) for the service. You activate the configured service so the customer can start using it. Finally, you send a provisioning confirmation to the customer.

If configuration fails partway through, you need to know exactly which network elements were already configured so you can retry without creating duplicate entries. If activation succeeds but the confirmation fails, the customer has working service but no notification. Without orchestration, you'd build a monolithic provisioning script that mixes order management, network configuration, and notification logic .  making it impossible to swap network vendors, test activation independently, or audit which order triggered which network changes.

## The Solution

**You just write the order creation, plan validation, network configuration, service activation, and customer notification logic. Conductor handles configuration retries, activation sequencing, and provisioning audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Order creation, validation, network configuration, activation, and confirmation workers each handle one step of turning up a new telecom service.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `tpv_activate` | Activates the configured service on the network so the customer can start using it. |
| **ConfigureWorker** | `tpv_configure` | Configures network resources (switches, routing, subscriber profiles) for the service type. |
| **ConfirmWorker** | `tpv_confirm` | Sends a provisioning confirmation to the customer with service ID and activation details. |
| **OrderWorker** | `tpv_order` | Creates a service order with customer ID and service type, returning an order ID. |
| **ValidateWorker** | `tpv_validate` | Validates the service order against the selected plan for compatibility and eligibility. |

Workers simulate telecom operations .  provisioning, activation, billing ,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tpv_order
    │
    ▼
tpv_validate
    │
    ▼
tpv_configure
    │
    ▼
tpv_activate
    │
    ▼
tpv_confirm
```

## Example Output

```
=== Example 815: Telecom Provisioning ===

Step 1: Registering task definitions...
  Registered: tpv_order, tpv_validate, tpv_configure, tpv_activate, tpv_confirm

Step 2: Registering workflow 'tpv_telecom_provisioning'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [activate] Processing
  [configure] Processing
  [confirm] Processing
  [order] Processing
  [validate] Processing

  Status: COMPLETED
  Output: {serviceId=..., activatedAt=..., configId=..., bandwidth=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/telecom-provisioning-1.0.0.jar
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
java -jar target/telecom-provisioning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tpv_telecom_provisioning \
  --version 1 \
  --input '{"customerId": "CUST-815", "CUST-815": "serviceType", "serviceType": "fiber-internet", "fiber-internet": "planId", "planId": "PLAN-100", "PLAN-100": "sample-PLAN-100"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tpv_telecom_provisioning -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real provisioning stack .  your OSS for order management, your NMS for network configuration, your BSS for customer notifications, and the workflow runs identically in production.

- **OrderWorker** (`tpv_order`): create the service order in your BSS/OSS (Amdocs, Netcracker, Oracle BRM) and return the order ID
- **ValidateWorker** (`tpv_validate`): validate against plan catalogs and customer eligibility rules in your product catalog system
- **ConfigureWorker** (`tpv_configure`): provision network resources via NETCONF/YANG, vendor NMS APIs, or HLR/HSS provisioning interfaces
- **ActivateWorker** (`tpv_activate`): activate the service in your OSS activation platform and update the subscriber profile in the network
- **ConfirmWorker** (`tpv_confirm`): send the customer a confirmation via SMS gateway, email, or push notification through your CRM

Connect your actual network management systems and the provisioning pipeline runs without structural changes.

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
telecom-provisioning-telecom-provisioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/telecomprovisioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TelecomProvisioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateWorker.java
│       ├── ConfigureWorker.java
│       ├── ConfirmWorker.java
│       ├── OrderWorker.java
│       └── ValidateWorker.java
└── src/test/java/telecomprovisioning/workers/
    ├── ActivateWorkerTest.java        # 1 tests
    └── OrderWorkerTest.java        # 1 tests
```
