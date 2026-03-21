# DNS Management in Java with Conductor

Orchestrates safe DNS record changes using [Conductor](https://github.com/conductor-oss/conductor). This workflow plans a DNS change, validates it against existing records for conflicts, applies it to the DNS provider, and verifies propagation.

## DNS Changes Without the Risk

Updating a DNS record sounds simple until you realize a typo can take down your entire domain. The change needs to be planned, validated for conflicts (does this CNAME clash with an existing A record?), applied to the DNS provider, and then verified to have propagated globally. Doing this manually in the Route53 console at 11 PM is how outages happen.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the DNS change logic. Conductor handles plan-validate-apply sequencing and propagation verification.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle safe DNS changes. Planning the record update, validating against conflicts, applying to the provider, and verifying propagation.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `dns_apply` | Pushes the validated DNS record changes to the DNS provider (e.g., Route53) |
| **PlanWorker** | `dns_plan` | Creates a DNS change plan based on the requested domain, record type, and target |
| **ValidateWorker** | `dns_validate` | Checks for conflicts with existing DNS records before applying the change |
| **VerifyWorker** | `dns_verify` | Confirms DNS propagation by querying resolvers to ensure the new records are live |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
dns_plan
    │
    ▼
dns_validate
    │
    ▼
dns_apply
    │
    ▼
dns_verify

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
java -jar target/dns-management-1.0.0.jar

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
java -jar target/dns-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dns_management_workflow \
  --version 1 \
  --input '{"domain": "sample-domain", "recordType": "standard", "target": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dns_management_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one DNS lifecycle step. replace the demo calls with Route53, Cloudflare, or Google Cloud DNS APIs, and the management workflow runs unchanged.

- **ApplyWorker** (`dns_apply`): call AWS Route53 ChangeResourceRecordSets, Cloudflare DNS API, or Google Cloud DNS to apply record changes
- **PlanWorker** (`dns_plan`): query your DNS provider's API to list existing records and generate a diff-based change plan
- **ValidateWorker** (`dns_validate`): use dnsjava or dig-style lookups to validate the planned records do not conflict with existing entries

Plug in Route53 or Cloudflare APIs and the DNS management workflow runs with the same contract.

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
dns-management-dns-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dnsmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── PlanWorker.java
│       ├── ValidateWorker.java
│       └── VerifyWorker.java
└── src/test/java/dnsmanagement/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
