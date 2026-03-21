# Hybrid Cloud Data Routing in Java Using Conductor :  Classify Sensitivity, Route to On-Prem or Cloud

A Java Conductor workflow example for hybrid cloud data routing. classifying incoming data by sensitivity level (PII, financial, public), then routing it to either on-premises infrastructure for sensitive workloads or cloud processing (AWS) for non-sensitive data. Uses [Conductor](https://github.

## Sensitive Data Can't Leave Your Data Center

Regulations like GDPR, HIPAA, and PCI-DSS require that certain data. patient records, financial transactions, PII,  stays within controlled environments. But running everything on-premises wastes cloud elasticity for workloads that have no compliance constraints. The challenge is automatically determining which data must stay on-prem and which can be processed in the cloud, then routing each record to the right infrastructure.

Without automated classification and routing, engineers make manual decisions about where data should go, or worse, everything runs in one place. either overpaying for on-prem infrastructure for non-sensitive workloads, or risking compliance violations by sending sensitive data to the cloud.

## The Solution

**You write the classification and processing logic. Conductor handles the sensitivity-based routing, retries, and compliance audit trail.**

`HybClassifyDataWorker` examines the data type and content to determine its sensitivity classification and target environment. `onprem` for sensitive data that must stay in the data center, `cloud` for everything else. A `SWITCH` task routes based on this classification: `HybProcessOnpremWorker` handles sensitive records within the on-premises environment, while `HybProcessCloudWorker` sends non-sensitive records to AWS for elastic processing. Conductor's conditional routing makes this classification-based split declarative, and every execution records the data ID, classification, and which path was taken,  giving you an audit trail for compliance.

### What You Write: Workers

Three workers handle the classification-and-routing split. Sensitivity classification, on-premises processing for regulated data, and cloud processing for non-sensitive workloads.

| Worker | Task | What It Does |
|---|---|---|
| **HybClassifyDataWorker** | `hyb_classify_data` | Classifies data by sensitivity (e.g., PII vs. general) and determines whether to route to on-prem or cloud |
| **HybProcessCloudWorker** | `hyb_process_cloud` | Processes non-sensitive data in the cloud (e.g., AWS us-east-1) with auto-scaled instances |
| **HybProcessOnpremWorker** | `hyb_process_onprem` | Processes sensitive/regulated data on-premises in the private datacenter with encryption |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
hyb_classify_data
    │
    ▼
SWITCH (hyb_switch_ref)
    ├── onprem: hyb_process_onprem
    ├── cloud: hyb_process_cloud

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
java -jar target/hybrid-cloud-1.0.0.jar

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
java -jar target/hybrid-cloud-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hybrid_cloud_demo \
  --version 1 \
  --input '{"dataId": "TEST-001", "dataType": "standard", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hybrid_cloud_demo -s COMPLETED -c 5

```

## How to Extend

Each worker addresses one routing concern. replace the simulated data classification with real DLP or Macie APIs and the on-prem-vs-cloud routing logic runs unchanged.

- **HybClassifyDataWorker** (`hyb_classify_data`): integrate AWS Macie for PII detection, Google Cloud DLP for sensitive data discovery, or custom regex/NLP classifiers for domain-specific sensitivity rules
- **HybProcessOnpremWorker** (`hyb_process_onprem`): process data on local Kubernetes clusters, call on-prem database APIs, or invoke services behind a VPN/private link
- **HybProcessCloudWorker** (`hyb_process_cloud`): submit jobs to AWS Lambda, ECS Fargate, or Google Cloud Run for elastic, pay-per-use processing of non-sensitive workloads

The classification and processing output shapes stay fixed. Swap simulated cloud calls for real AWS or Azure APIs and the sensitivity-based routing runs unchanged.

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
hybrid-cloud/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/hybridcloud/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HybridCloudExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HybClassifyDataWorker.java
│       ├── HybProcessCloudWorker.java
│       └── HybProcessOnpremWorker.java
└── src/test/java/hybridcloud/workers/
    ├── HybClassifyDataWorkerTest.java        # 4 tests
    ├── HybProcessCloudWorkerTest.java        # 4 tests
    └── HybProcessOnpremWorkerTest.java        # 4 tests

```
