# Release Management in Java with Conductor :  Preparation, Approval Gates, Deployment, and Announcements

Orchestrates the software release lifecycle using [Conductor](https://github.com/conductor-oss/conductor). This workflow prepares a release by tagging the version and building artifacts, gates it through an approval step before anything goes to production, deploys the approved release, and announces the release to stakeholders via configured channels.

## Shipping Without the Chaos

Version 2.4.0 is ready to ship. Someone needs to tag the commit, build release artifacts, get sign-off from the release manager, deploy to production, and announce the release to customers and internal teams. If deployment fails after approval, the release is in limbo. Approved but not deployed. If the announcement goes out before deployment actually succeeds, customers expect features that aren't live yet. Every step must happen in the right order, and every step's success must be confirmed before moving on.

Without orchestration, you'd manage releases via a checklist in a Confluence page and a sequence of manual commands. Deployments happen in someone's terminal with no audit trail. Approvals live in Slack threads that get lost. There's no record of which version was deployed when, whether it was approved, or who announced it.

## The Solution

**You write the release preparation and deployment logic. Conductor handles approval gating, deploy-before-announce sequencing, and the full release audit trail.**

Each stage of the release pipeline is a simple, independent worker. The preparer tags the version, builds release artifacts, and compiles the changelog. The approver gates the release: checking that all tests pass, the change log is complete, and the release manager has signed off. The deployer pushes the approved artifacts to production infrastructure. The announcer notifies stakeholders, posting release notes to Slack, updating the status page, and sending customer-facing changelogs. Conductor executes them in strict sequence, ensures deployment only happens after approval passes, retries if the deployment target is temporarily unavailable, and provides a complete audit trail of every release. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage the release lifecycle. Preparing the version, gating through approval, deploying to production, and announcing to stakeholders.

| Worker | Task | What It Does |
|---|---|---|
| **AnnounceWorker** | `rm_announce` | Publishes release notes and notifies stakeholders (email, Slack, status page) |
| **ApproveWorker** | `rm_approve` | Gates the release with release-manager approval before production deployment |
| **DeployWorker** | `rm_deploy` | Deploys the approved version to the production environment |
| **PrepareWorker** | `rm_prepare` | Gathers changelogs, feature counts, and bug fixes into a release summary (e.g., "12 changes, 3 fixes") |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
rm_prepare
    │
    ▼
rm_approve
    │
    ▼
rm_deploy
    │
    ▼
rm_announce

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
java -jar target/release-management-1.0.0.jar

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
java -jar target/release-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow release_management_workflow \
  --version 1 \
  --input '{"version": "1.0", "product": "widget-pro"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w release_management_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one release lifecycle stage. replace the simulated calls with GitHub Actions, ArgoCD, or Statuspage.io APIs for real builds, deployments, and announcements, and the release workflow runs unchanged.

- **PrepareWorker** → automate real release prep: tag Git commits via GitHub/GitLab API, trigger CI builds in Jenkins/GitHub Actions/CircleCI, generate changelogs from conventional commits, and publish artifacts to Artifactory/Nexus
- **ApproveWorker** → implement real approval gates: use Conductor's WAIT task for manual sign-off, check Jira release tickets for approval status, or enforce automated gates (all tests green, no P0 bugs open, security scan passed)
- **DeployWorker** → deploy to real infrastructure: trigger Argo CD sync, Spinnaker pipeline, Helm upgrade, or AWS CodeDeploy, with health check verification before marking the deployment complete
- **AnnounceWorker** → notify via real channels: post release notes to Slack via webhook, update Statuspage.io, send customer-facing emails via SendGrid, or publish to an RSS/changelog feed

Integrate with your CI system and Slack API; the release workflow keeps the same interface between steps.

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
release-management-release-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/releasemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnnounceWorker.java
│       ├── ApproveWorker.java
│       ├── DeployWorker.java
│       └── PrepareWorker.java
└── src/test/java/releasemanagement/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
