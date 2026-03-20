# Service Activation in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom service activation .  validating a service order against the customer's account, provisioning network resources for the requested service type, running automated service tests, activating the service on the network, and notifying the customer that their service is live. Uses [Conductor](https://github.## Why Service Activation Needs Orchestration

Activating a new telecom service requires a strict sequence where each step must succeed before proceeding. You validate the service order .  confirming the order exists, the customer's account is in good standing, and the requested service type is available at their location. You provision the network resources ,  creating subscriber profiles in the HLR/HSS, configuring access ports, and allocating bandwidth. You test the provisioned service by running automated checks to confirm connectivity and quality. You activate the service so the customer can start using it. Finally, you notify the customer with their service ID and activation details.

If provisioning succeeds but the test reveals a quality issue, you need the provisioned service ID to troubleshoot without re-provisioning. If activation succeeds but notification fails, the customer has working service but no confirmation. Without orchestration, you'd build a monolithic activation script that mixes order validation, network provisioning, test automation, and CRM notification .  making it impossible to swap provisioning platforms, test service quality independently, or audit which order triggered which network changes.

## The Solution

**You just write the order validation, network provisioning, service testing, activation, and customer notification logic. Conductor handles resource allocation retries, configuration deployment, and activation audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Order validation, resource allocation, configuration deployment, and service verification workers each handle one step of activating customer services.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `sac_activate` | Activates the provisioned and tested service on the network so the customer can start using it. |
| **NotifyWorker** | `sac_notify` | Sends activation confirmation to the customer with service ID and connection details. |
| **ProvisionWorker** | `sac_provision` | Provisions network resources for the service type .  subscriber profiles, access ports, bandwidth allocation. |
| **ValidateOrderWorker** | `sac_validate_order` | Validates the service order against the customer's account .  checking order existence, account status, and availability. |

Workers simulate telecom operations .  provisioning, activation, billing ,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
sac_validate_order
    │
    ▼
sac_provision
    │
    ▼
sac_test
    │
    ▼
sac_activate
    │
    ▼
sac_notify
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
java -jar target/service-activation-1.0.0.jar
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
java -jar target/service-activation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sac_service_activation \
  --version 1 \
  --input '{"orderId": "TEST-001", "customerId": "TEST-001", "serviceType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sac_service_activation -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real activation systems .  your CRM for order validation, your HLR/HSS for subscriber provisioning, your test automation platform for service verification, and the workflow runs identically in production.

- **ValidateOrderWorker** (`sac_validate_order`): validate the order in your BSS/OSS (Amdocs, Netcracker, Oracle BRM) .  checking customer account status, credit, and service availability at the location
- **ProvisionWorker** (`sac_provision`): provision the service in your OSS activation platform by creating subscriber profiles in HLR/HSS, configuring access equipment via NETCONF/YANG, and allocating bandwidth
- **TestWorker** (`sac_test`): run automated service quality tests using your test automation platform (e.g., EXFO, Spirent) to verify connectivity, throughput, and latency meet service-level targets
- **ActivateWorker** (`sac_activate`): flip the service to active in your OSS inventory, enabling traffic flow and updating the subscriber's profile status in the HLR/HSS
- **NotifyWorker** (`sac_notify`): send the activation confirmation via SMS gateway, email, push notification, or update the customer's self-service portal through your CRM

Change resource allocation strategies or configuration tools and the activation pipeline keeps its structure.

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
service-activation-service-activation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/serviceactivation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceActivationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateWorker.java
│       ├── NotifyWorker.java
│       ├── ProvisionWorker.java
│       └── ValidateOrderWorker.java
└── src/test/java/serviceactivation/workers/
    ├── ActivateWorkerTest.java        # 1 tests
    └── ValidateOrderWorkerTest.java        # 1 tests
```
