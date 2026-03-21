# Disaster Recovery in Java with Conductor

Orchestrates a full disaster recovery failover using [Conductor](https://github.com/conductor-oss/conductor). When the primary region goes down, this workflow detects the failure, promotes the standby database in the DR region, updates DNS to point traffic to the backup, and verifies the recovery. Tracking RTO throughout.

## When the Primary Region Goes Down

Your primary region (us-east-1) suffers an outage. The database needs to be failed over to the standby in us-west-2, DNS records must be updated to redirect traffic, and someone needs to verify the DR region is healthy, all within your RTO target. Doing these steps manually under pressure risks mistakes, missed steps, and blown SLAs.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the failover logic. Conductor handles step ordering, RTO tracking, and guaranteed recovery completion.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the DR failover sequence. Detecting the outage, promoting the standby database, updating DNS, and verifying recovery within RTO.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `dr_detect` | Confirms the primary region failure by checking health endpoints and marks the outage as verified |
| **FailoverDbWorker** | `dr_failover_db` | Promotes the standby database replica in the DR region to primary |
| **UpdateDnsWorker** | `dr_update_dns` | Updates DNS records to redirect traffic from the failed primary to the DR region |
| **VerifyWorker** | `dr_verify` | Validates the DR region is healthy and serving traffic, and reports the achieved RTO in minutes |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
dr_detect
    │
    ▼
dr_failover_db
    │
    ▼
dr_update_dns
    │
    ▼
dr_verify

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
java -jar target/disaster-recovery-1.0.0.jar

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
java -jar target/disaster-recovery-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow disaster_recovery_workflow \
  --version 1 \
  --input '{"primaryRegion": "us-east-1", "drRegion": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w disaster_recovery_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one failover step. replace the demo calls with AWS RDS promote-replica, Route53, or Azure Service Health APIs, and the DR workflow runs unchanged.

- **DetectWorker** (`dr_detect`): integrate with AWS Health API, Azure Service Health, or synthetic monitoring (e.g., Pingdom, Checkly) to detect region failures
- **FailoverDbWorker** (`dr_failover_db`): call AWS RDS promote-read-replica, Aurora Global Database failover, or equivalent cloud DB failover APIs
- **UpdateDnsWorker** (`dr_update_dns`): update Route53 failover records, Cloudflare DNS, or your DNS provider's API to reroute traffic

Wire in your actual cloud provider and DNS APIs; the failover workflow keeps the same interface.

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
disaster-recovery-disaster-recovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/disasterrecovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectWorker.java
│       ├── FailoverDbWorker.java
│       ├── UpdateDnsWorker.java
│       └── VerifyWorker.java
└── src/test/java/disasterrecovery/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
