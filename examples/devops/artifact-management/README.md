# Artifact Management in Java with Conductor :  Build, Sign, Publish, Cleanup

Build artifact lifecycle orchestration: build, sign, publish, and cleanup old artifacts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Build Artifacts Need a Managed Lifecycle

After building a JAR, Docker image, or npm package, the artifact needs signing (so consumers can verify it hasn't been tampered with), publishing to a repository (Artifactory, Docker Hub, npm registry), and old versions need cleanup (keeping the last 10 versions, deleting artifacts older than 90 days). Without lifecycle management, artifact repositories grow unbounded, unsigned artifacts create supply chain risks, and there's no audit trail of what was published when.

Each step has dependencies: signing requires the build output, publishing requires the signed artifact, and cleanup requires knowing which versions are still in use. If publishing fails, the artifact is built and signed but not available .  retrying should resume from publishing, not rebuild.

## The Solution

**You write the build, sign, and publish logic. Conductor handles artifact lifecycle sequencing, provenance tracking, and retention enforcement.**

`BuildWorker` compiles the source code and produces the build artifact. JAR, Docker image, or package .  with version metadata. `SignWorker` signs the artifact for integrity verification using GPG, Sigstore, or Docker Content Trust. `PublishWorker` uploads the signed artifact to the target repository with version tags and metadata. `CleanupWorker` applies retention policies ,  removing old versions, cleaning up unused tags, and reclaiming storage. Conductor sequences these steps and records the complete artifact provenance chain.

### What You Write: Workers

Four workers manage the artifact lifecycle. Building the package, signing for integrity, publishing to a repository, and enforcing retention cleanup.

| Worker | Task | What It Does |
|---|---|---|
| **BuildWorker** | `am_build` | Builds the project artifact. |
| **CleanupWorker** | `am_cleanup` | Cleans up old artifacts beyond retention policy. |
| **PublishWorker** | `am_publish` | Publishes the signed artifact to Artifactory. |
| **SignWorker** | `am_sign` | Signs the built artifact with GPG key. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
am_build
    │
    ▼
am_sign
    │
    ▼
am_publish
    │
    ▼
am_cleanup

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
java -jar target/artifact-management-1.0.0.jar

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
java -jar target/artifact-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow artifact_management_workflow \
  --version 1 \
  --input '{"project": "sample-project", "version": "1.0"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w artifact_management_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one artifact lifecycle step .  replace the simulated calls with Maven/Gradle builds, Sigstore signing, or JFrog Artifactory publishing, and the management workflow runs unchanged.

- **BuildWorker** (`am_build`): integrate with Maven/Gradle for JAR builds, Docker buildx for multi-architecture images, or npm pack for JavaScript packages
- **SignWorker** (`am_sign`): use Sigstore/cosign for keyless signing of container images, GPG for JAR signing, or AWS Signer for code signing with audit trails
- **PublishWorker** (`am_publish`): push to JFrog Artifactory, Docker Hub, AWS ECR, or GitHub Packages with proper version tagging and provenance metadata
- **CleanupWorker** (`am_cleanup`): apply retention policies via the Artifactory AQL API, Docker Hub tag deletion, or ECR lifecycle policies, keeping the last N versions and deleting artifacts older than the configured threshold

Plug in Artifactory or Docker Hub APIs; the build-sign-publish pipeline keeps the same data contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
artifact-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/artifactmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ArtifactManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BuildWorker.java
│       ├── CleanupWorker.java
│       ├── PublishWorker.java
│       └── SignWorker.java
└── src/test/java/artifactmanagement/workers/
    ├── BuildWorkerTest.java        # 6 tests
    ├── CleanupWorkerTest.java        # 4 tests
    ├── PublishWorkerTest.java        # 3 tests
    └── SignWorkerTest.java        # 4 tests

```
