# Orkes Cloud Connection Test in Java with Conductor: Verify Your Cloud Environment

A minimal Java Conductor workflow that verifies your connection to Orkes Cloud, the managed Conductor service. A single greet task confirms that your API key, secret, and server URL are correctly configured, and that your local workers can poll tasks from the Orkes Cloud environment. Uses [Orkes Cloud](https://orkes.io/) as the managed Conductor server.

## Connecting to Orkes Cloud

Orkes Cloud runs Conductor as a managed service.; no Docker, no infrastructure. But you need to verify three things before building workflows: your API credentials are valid, your worker can reach the Orkes Cloud server, and task polling works end-to-end. This single-task workflow confirms all three.

If the workflow completes, your Orkes Cloud connection is working. If it fails, the error tells you whether the issue is authentication, connectivity, or worker configuration.

## The Solution

**One worker, one task, one verification.**

A single greet worker polls the Orkes Cloud Conductor server, executes a task, and returns the result. A successful run means your entire Orkes Cloud setup: credentials, connectivity, and worker registration, is correctly configured.

### What You Write: Workers

One verification worker confirms connectivity to your Orkes Cloud environment, validating credentials and endpoint configuration.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **CloudGreetWorker** | `` | Worker that greets a user with a cloud/local indicator. In cloud mode, the task name is "cloud_greet" and the greetin... | Simulated |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the simulated logic for your real service calls, the worker contract stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cloud_greet
```

## Example Output

```
=== Orkes Conductor Cloud Connection Example ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'orkes_cloud'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: 9aa5a789-1e5b-d257-4cce-02933c1151ce

  [] [

  Status: COMPLETED
  Output: {greeting=${cloud_greet_ref.output.greeting}, mode=${cloud_greet_ref.output.mode}}

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
java -jar target/orkes-cloud-1.0.0.jar
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
java -jar target/orkes-cloud-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow orkes_cloud_test \
  --version 1 \
  --input '{"name": "John Doe"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w orkes_cloud_test -s COMPLETED -c 5
```

## How to Extend

Once this connection test passes, your Orkes Cloud credentials, server URL, and worker polling are verified. Start building production workflows against your managed Conductor instance.

- **CloudGreetWorker** (`cloud_greet` / `local_greet`): connect to your Orkes Cloud Conductor instance with API key authentication for production workloads, or use Conductor OSS for self-hosted deployments

The verification worker confirms cloud connectivity independently of any application workflows you deploy later.

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
orkes-cloud/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/orkescloud/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OrkesCloudExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── CloudGreetWorker.java
└── src/test/java/orkescloud/workers/
    └── CloudGreetWorkerTest.java        # 8 tests
```
