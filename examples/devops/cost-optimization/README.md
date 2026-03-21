# Cloud Cost Optimization in Java with Conductor :  Billing Collection, Usage Analysis, Savings Recommendations, and Auto-Apply

Orchestrates cloud cost optimization using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects billing data for a specified account and period, analyzes resource utilization to identify waste (idle instances, over-provisioned databases, unused EBS volumes), generates savings recommendations with estimated dollar impact, and auto-applies safe optimizations like purchasing reserved instances or deleting orphaned snapshots.

## The Cloud Bill Problem

Your AWS bill jumped 40% last month. Somewhere in your account, there are EC2 instances running at 5% CPU, over-provisioned RDS databases with 2TB allocated but only 50GB used, and EBS snapshots from instances deleted six months ago. Finding these wastes requires collecting billing data, cross-referencing it with utilization metrics, computing how much each optimization would save, and then actually applying the changes. Rightsizing instances, deleting orphaned resources, converting on-demand to reserved capacity. Missing even one step means money leaking out every hour.

Without orchestration, you'd write a cost analysis script that pulls billing data, scans for waste, and maybe sends an email with recommendations. But nobody acts on the email. The recommendations go stale because utilization patterns change daily. There's no record of which optimizations were applied, how much they actually saved, or whether the changes caused performance issues.

## The Solution

**You write the billing analysis and optimization logic. Conductor handles the collection-to-savings pipeline and tracks every dollar saved.**

Each stage of the cost optimization pipeline is a simple, independent worker. The billing collector pulls spend data for the specified account and period from the cloud provider's cost API. The usage analyzer cross-references billing with utilization metrics to identify waste: instances running below 10% CPU, databases with unused storage, orphaned load balancers with no targets. The recommender generates prioritized savings opportunities with estimated monthly dollar impact for each. The savings applier executes safe optimizations automatically, deleting orphaned snapshots, rightsizing instances, purchasing reserved capacity, and reports what was applied and the projected savings. Conductor executes them in strict sequence, retries if the billing API is rate-limited, and tracks spend amounts and savings amounts at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers run the cost optimization cycle. Collecting billing data, analyzing resource utilization, generating savings recommendations, and auto-applying safe optimizations.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeUsageWorker** | `co_analyze_usage` | Cross-references billing with utilization to find waste: idle instances, oversized volumes, orphaned resources |
| **ApplySavingsWorker** | `co_apply_savings` | Executes safe optimizations: right-sizes instances, terminates idle resources, purchases reserved capacity |
| **CollectBillingWorker** | `co_collect_billing` | Pulls billing and spend data for the specified account and period from the cloud cost API |
| **RecommendWorker** | `co_recommend` | Generates prioritized savings recommendations with estimated dollar impact per optimization |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
co_collect_billing
    │
    ▼
co_analyze_usage
    │
    ▼
co_recommend
    │
    ▼
co_apply_savings

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
java -jar target/cost-optimization-1.0.0.jar

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
java -jar target/cost-optimization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cost_optimization_workflow \
  --version 1 \
  --input '{"account": 10, "period": "sample-period"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cost_optimization_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one optimization stage .  plug in AWS Cost Explorer, CloudWatch utilization metrics, or Trusted Advisor for real billing analysis and rightsizing, and the cost workflow runs unchanged.

- **CollectBillingWorker** → pull real billing data: AWS Cost Explorer API, GCP Billing Export to BigQuery, Azure Cost Management API, or multi-cloud aggregators like CloudHealth or Spot.io
- **AnalyzeUsageWorker** → correlate real utilization: CloudWatch metrics for EC2/RDS, GCP Monitoring for Compute Engine, or Kubernetes metrics-server for pod-level CPU/memory analysis
- **RecommendWorker** → generate real recommendations: AWS Trusted Advisor, AWS Compute Optimizer, GCP Recommender API, or custom logic that compares actual utilization against instance type pricing
- **ApplySavingsWorker** → execute real optimizations: AWS SDK to modify/terminate instances, purchase Savings Plans or Reserved Instances, delete unused EBS volumes, or apply Kubernetes resource limits

Connect to the AWS Cost Explorer API and the savings pipeline produces real recommendations without workflow changes.

**Add new stages** by inserting tasks in `workflow.json`, for example, a budget alerting step that checks if current spend exceeds thresholds, an approval step (using Conductor's WAIT task) that requires finance team sign-off before purchasing reservations, or a tracking step that records applied savings and compares them against projections over time.

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
cost-optimization-cost-optimization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/costoptimization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeUsageWorker.java
│       ├── ApplySavingsWorker.java
│       ├── CollectBillingWorker.java
│       └── RecommendWorker.java
└── src/test/java/costoptimization/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
