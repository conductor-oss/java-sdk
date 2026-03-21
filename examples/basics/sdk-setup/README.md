# SDK Setup Verification in Java with Conductor: Smoke Test for Your Maven/Java Configuration

A minimal Java Conductor workflow that verifies your SDK setup is correct, the Maven dependency is properly declared, the `conductor-client` JAR resolves, the `ConductorClient` can connect to the server, and a worker can poll and execute tasks. If this runs, your Java development environment is ready for Conductor development. Uses [Conductor](https://github.com/conductor-oss/conductor) to validate the end-to-end SDK setup.

## Verifying Your Java SDK Configuration

Setting up the Conductor Java SDK involves several pieces: adding the `conductor-client` Maven dependency, configuring the server URL, creating a `ConductorClient` instance, and registering workers. If any piece is misconfigured, you'll get cryptic errors when you try to build a real workflow. This smoke test catches setup issues early.

A successful run confirms: Maven resolves the dependency, the client connects to Conductor, the worker registers and polls, and task execution completes.

## The Solution

**Run this smoke test first.**

One worker, one task. Just enough to verify your entire SDK setup. If it passes, start building. If it fails, the error tells you exactly which part of your setup needs fixing.

### What You Write: Workers

A minimal worker runs a round-trip through the SDK to confirm that your Maven dependencies, Java version, and Conductor connection are all configured correctly.

| Worker | Task | What It Does |
|---|---|---|
| `SdkTestWorker` | `sdk_test_task` | Accepts a `check` string input (defaults to "default" if blank), returns a confirmation message that conductor-client 5.0.1 is working |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
sdk_test_task

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
java -jar target/sdk-setup-1.0.0.jar

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
java -jar target/sdk-setup-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sdk_setup_test \
  --version 1 \
  --input '{"check": "connectivity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sdk_setup_test -s COMPLETED -c 5

```

## How to Extend

Once this smoke test passes, your Maven dependency, client configuration, and worker polling are verified. Start building real workers with the same SDK setup.

The smoke test confirms your setup works. Build your real workers on the same foundation.

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
sdk-setup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sdksetup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SdkSetupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
└── src/test/java/sdksetup/workers/
    └── SdkTestWorkerTest.java        # 5 tests

```
