# Implementing Multi-Step Compensation in Java with Conductor :  Account Provisioning with Reverse-Order Undo

A Java Conductor workflow example demonstrating multi-step compensation .  creating an account, setting up billing, and provisioning resources in a forward workflow, with a separate compensation workflow that undoes all steps in reverse order when any step fails.

## The Problem

You need to provision a new customer account .  create the account record, set up billing, and provision cloud resources. If resource provisioning fails (quota exceeded, region unavailable), the billing setup must be undone and the account must be deleted ,  in reverse order. Each undo operation must receive the output from the original forward step (account ID, billing ID) to know what to clean up.

Without orchestration, compensation logic is tangled with forward logic. Each step must know about every other step's undo operation. Partial failures leave orphaned billing records attached to deleted accounts, or provisioned resources with no associated billing. Testing the compensation path requires simulating failures at each step, which is nearly impossible in a monolithic script.

## The Solution

**You just write the provisioning steps and their matching undo operations. Conductor handles forward sequencing, automatic compensation in reverse order via the failure workflow, retries on each undo step, and a complete record of both the forward and compensation paths.**

The forward workflow runs three workers in sequence .  create account, setup billing, provision resources. A separate compensation workflow runs the undo workers in reverse order (undo provision, undo billing, undo account) using the outputs from the forward steps. Conductor's failure workflow feature links them: when the forward workflow fails, compensation runs automatically. Every step in both directions is tracked. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three forward workers. CreateAccountWorker, SetupBillingWorker, and ProvisionResourcesWorker. Run in sequence, with matching undo workers (UndoProvisionWorker, UndoBillingWorker, UndoAccountWorker) that execute in reverse order when any step fails.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAccountWorker** | `msc_create_account` | Worker for msc_create_account .  creates an account and returns a deterministic accountId. |
| **ProvisionResourcesWorker** | `msc_provision_resources` | Worker for msc_provision_resources .  provisions resources. If failAt input equals "provision", the task returns FAILE.. |
| **SetupBillingWorker** | `msc_setup_billing` | Worker for msc_setup_billing .  sets up billing and returns a deterministic billingId. |
| **UndoAccountWorker** | `msc_undo_account` | Worker for msc_undo_account .  undoes account creation. |
| **UndoBillingWorker** | `msc_undo_billing` | Worker for msc_undo_billing .  undoes billing setup. |
| **UndoProvisionWorker** | `msc_undo_provision` | Worker for msc_undo_provision .  undoes resource provisioning. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
Input -> CreateAccountWorker -> ProvisionResourcesWorker -> SetupBillingWorker -> UndoAccountWorker -> UndoBillingWorker -> UndoProvisionWorker -> Output

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
java -jar target/multi-step-compensation-1.0.0.jar

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
java -jar target/multi-step-compensation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_step_compensation \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_step_compensation -s COMPLETED -c 5

```

## How to Extend

Each worker performs one provisioning action .  connect the account creator to your identity system, the billing worker to Stripe, the resource provisioner to AWS or GCP, and the forward-plus-reverse-compensation workflow stays the same.

- **CreateAccountWorker** (`msc_create_account`): create a real account in your user database (Postgres, DynamoDB) or identity provider (Auth0, Cognito)
- **ProvisionResourcesWorker** (`msc_provision_resources`): provision real cloud resources (AWS EC2/ECS, Kubernetes namespaces, S3 buckets) via SDK calls
- **SetupBillingWorker** (`msc_setup_billing`): create a real billing subscription in Stripe, Chargebee, or your billing system

Connect provisioning workers to your real identity provider, billing system, and cloud APIs, and the forward-plus-reverse-compensation flow adapts to production without modification.

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
multi-step-compensation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multistepcompensation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiStepCompensationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateAccountWorker.java
│       ├── ProvisionResourcesWorker.java
│       ├── SetupBillingWorker.java
│       ├── UndoAccountWorker.java
│       ├── UndoBillingWorker.java
│       └── UndoProvisionWorker.java

```
