# Infrastructure Provisioning in Java with Conductor :  Plan, Validate, Provision, Configure, Verify

Orchestrates infrastructure provisioning: plan, validate, provision, configure, and verify across cloud providers. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Infrastructure Provisioning Needs Guard Rails

An engineer requests 3 EC2 instances in us-east-1. Before spinning them up, the request needs validation: Does the account have sufficient quota? Does the instance type comply with organization policies (no m5.24xlarge without VP approval)? Is the VPC and subnet configuration correct? Will this push the monthly bill over budget?

After provisioning, the instances need configuration: install monitoring agents, configure security groups, join the service mesh, register with service discovery. If provisioning succeeds but configuration fails, you have running instances that aren't monitored or secured .  a dangerous state that must be detected and remediated. Every provisioning action needs a complete audit trail for cost tracking and compliance.

## The Solution

**You write the provisioning and policy validation logic. Conductor handles the plan-validate-provision-configure-verify pipeline and the full infrastructure audit trail.**

`PlanWorker` generates the infrastructure plan .  resource types, sizes, regions, networking, and estimated cost. `ValidateWorker` checks the plan against organizational policies, quota limits, budget constraints, and security requirements. `ProvisionWorker` creates the validated resources ,  launching instances, creating databases, provisioning load balancers. `ConfigureWorker` sets up the provisioned resources ,  installing agents, configuring security, joining service meshes, and setting up monitoring. `VerifyWorker` confirms all resources are operational ,  health checks passing, monitoring reporting, and traffic routing correctly. Conductor sequences these five steps and records the complete provisioning audit trail.

### What You Write: Workers

Five workers manage provisioning. Planning resources, validating against policies, provisioning infrastructure, configuring instances, and verifying health.

| Worker | Task | What It Does |
|---|---|---|
| **Configure** | `ip_configure` | Applies configuration to the provisioned resource. |
| **Plan** | `ip_plan` | Creates an infrastructure provisioning plan. |
| **Provision** | `ip_provision` | Provisions the infrastructure resource. |
| **Validate** | `ip_validate` | Validates the infrastructure plan against policies. |
| **Verify** | `ip_verify` | Verifies the provisioned resource is healthy. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
ip_plan
    │
    ▼
ip_validate
    │
    ▼
ip_provision
    │
    ▼
ip_configure
    │
    ▼
ip_verify

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
java -jar target/infrastructure-provisioning-1.0.0.jar

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
java -jar target/infrastructure-provisioning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow infra_provisioning_workflow \
  --version 1 \
  --input '{"environment": "staging", "region": "us-east-1", "resourceType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w infra_provisioning_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one provisioning step .  replace the simulated calls with Terraform, AWS CloudFormation, or Ansible for real resource creation and configuration, and the provisioning workflow runs unchanged.

- **Plan** (`ip_plan`): generate a resource plan using Terraform plan, Pulumi preview, or CloudFormation change sets, showing what will be created, modified, or destroyed
- **Validate** (`ip_validate`): integrate with OPA (Open Policy Agent) for policy enforcement, check AWS Service Quotas API for quota limits, and estimate costs via AWS Pricing API
- **Provision** (`ip_provision`): use Terraform apply, AWS CloudFormation, Pulumi, or AWS SDK calls for real resource provisioning with state management
- **Configure** (`ip_configure`): run Ansible playbooks for instance configuration, register with Consul/Eureka for service discovery, and install Datadog/New Relic agents for monitoring
- **Verify** (`ip_verify`): run health checks against provisioned resources, verify network connectivity, and confirm services are responding before marking the infrastructure as ready

Replace with Terraform and Ansible for real provisioning; the five-stage pipeline maintains the same data contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
infrastructure-provisioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/infrastructureprovisioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InfrastructureProvisioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Configure.java
│       ├── Plan.java
│       ├── Provision.java
│       ├── Validate.java
│       └── Verify.java
└── src/test/java/infrastructureprovisioning/workers/
    ├── PlanTest.java        # 7 tests
    ├── ProvisionTest.java        # 7 tests
    └── VerifyTest.java        # 7 tests

```
