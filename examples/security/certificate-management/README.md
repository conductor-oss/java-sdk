# Implementing Certificate Management in Java with Conductor :  Inventory, Expiry Assessment, Renewal, and Distribution

A Java Conductor workflow example for TLS certificate management .  inventorying all certificates, assessing which are approaching expiry, renewing certificates before they expire, and distributing renewed certificates to servers.

## The Problem

You manage TLS certificates across your infrastructure .  web servers, APIs, load balancers, internal services. Certificates expire, and expired certificates cause outages and security warnings. You need to inventory all certificates, identify those approaching expiry within a renewal window, renew them (via Let's Encrypt, internal CA, or commercial CA), and distribute the renewed certificates to every server that uses them.

Without orchestration, certificate management is a calendar reminder that someone set once and that nobody maintains. Certificates expire without warning, causing 3 AM outages. Renewal is manual .  generate CSR, submit to CA, wait, download cert, deploy to 15 servers. Miss one server and users get security errors.

## The Solution

**You just write the CA renewal and cert deployment logic. Conductor handles the renewal sequence, retries when CAs are temporarily unreachable, and a complete record of every certificate inventoried, renewed, and deployed.**

Each certificate step is an independent worker .  inventory, expiry assessment, renewal, and distribution. Conductor runs them in sequence: inventory all certs, assess which need renewal, renew them, then distribute. Every certificate operation is tracked with cert details, expiry dates, renewal status, and deployment targets. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers cover the certificate lifecycle: InventoryWorker discovers all TLS certs, AssessExpiryWorker identifies those nearing expiration, RenewWorker obtains fresh certificates, and DistributeWorker deploys them to every server that needs them.

| Worker | Task | What It Does |
|---|---|---|
| **AssessExpiryWorker** | `cm_assess_expiry` | Identifies certificates expiring within the renewal window |
| **DistributeWorker** | `cm_distribute` | Distributes renewed certificates to all affected endpoints |
| **InventoryWorker** | `cm_inventory` | Scans all environments to discover and inventory every certificate |
| **RenewWorker** | `cm_renew` | Renews expiring certificates through the certificate authority |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
cm_inventory
    │
    ▼
cm_assess_expiry
    │
    ▼
cm_renew
    │
    ▼
cm_distribute

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
java -jar target/certificate-management-1.0.0.jar

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
java -jar target/certificate-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow certificate_management_workflow \
  --version 1 \
  --input '{"scope": "sample-scope", "renewalWindow": "sample-renewalWindow"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w certificate_management_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker manages one certificate lifecycle step .  connect RenewWorker to Let's Encrypt or your internal CA, DistributeWorker to deploy certs across nginx and Kubernetes Secrets, and the inventory-renew-distribute workflow stays the same.

- **AssessExpiryWorker** (`cm_assess_expiry`): check certificate expiry dates via OpenSSL or cloud provider APIs, flag those within the renewal window
- **DistributeWorker** (`cm_distribute`): deploy renewed certificates to nginx, Apache, HAProxy, Kubernetes Secrets, and cloud load balancers
- **InventoryWorker** (`cm_inventory`): scan load balancers, web servers, and Kubernetes TLS secrets to build a real certificate inventory

Point RenewWorker at Let's Encrypt or your internal CA, and the inventory-renew-distribute flow works without touching the workflow definition.

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
certificate-management-certificate-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/certificatemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessExpiryWorker.java
│       ├── DistributeWorker.java
│       ├── InventoryWorker.java
│       └── RenewWorker.java
└── src/test/java/certificatemanagement/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
