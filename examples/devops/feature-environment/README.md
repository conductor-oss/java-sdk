# Feature Environment in Java with Conductor

Automates on-demand feature environment provisioning using [Conductor](https://github.com/conductor-oss/conductor). This workflow provisions an isolated Kubernetes namespace for a feature branch, deploys the branch code, configures a preview DNS URL, and posts the preview link back to the pull request.

## Preview Environments for Every PR

A developer opens a pull request for `feature-auth-v2`. Reviewers want to click a link and see it running. Not read diffs. The workflow provisions a namespace, deploys the branch, sets up a preview URL like `feature-auth-v2.preview.example.com`, and comments the link on the PR. When the PR is merged, the environment gets torn down.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the provisioning and deployment logic. Conductor handles namespace-to-notification sequencing, retries, and environment lifecycle tracking.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers provision the preview environment. Creating the namespace, deploying the branch, configuring DNS, and notifying the pull request.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureDnsWorker** | `fe_configure_dns` | Sets up a preview DNS record (e.g., `branch-name.preview.example.com`) pointing to the deployed environment |
| **DeployBranchWorker** | `fe_deploy_branch` | Builds and deploys the feature branch code to the provisioned preview environment |
| **NotifyWorker** | `fe_notify` | Posts the preview URL as a comment on the pull request so reviewers can access it |
| **ProvisionWorker** | `fe_provision` | Creates an isolated Kubernetes namespace (or VM/container) for the feature branch |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
fe_provision
    │
    ▼
fe_deploy_branch
    │
    ▼
fe_configure_dns
    │
    ▼
fe_notify

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
java -jar target/feature-environment-1.0.0.jar

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
java -jar target/feature-environment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow feature_environment_workflow \
  --version 1 \
  --input '{"branch": "sample-branch", "repository": "sample-repository"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w feature_environment_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one provisioning step. replace the simulated calls with Kubernetes namespace APIs, Helm deployments, or GitHub PR comment APIs, and the environment workflow runs unchanged.

- **ProvisionWorker** (`fe_provision`): create Kubernetes namespaces via the K8s API, Terraform workspaces for cloud-native isolation, or Vercel/Netlify preview deployments for frontend branches
- **DeployBranchWorker** (`fe_deploy_branch`): build Docker images from the branch, push to a registry, and deploy via Helm upgrade, kubectl apply, or Argo CD ApplicationSets targeting the preview namespace
- **ConfigureDnsWorker** (`fe_configure_dns`): create Route53 CNAME records, Cloudflare DNS entries, or Kubernetes Ingress rules that route `branch-name.preview.example.com` to the deployed service
- **NotifyWorker** (`fe_notify`): post the preview URL as a GitHub PR comment via the GitHub API, send a Slack message to the team channel, or update the JIRA ticket with the environment link

Connect to the Kubernetes API and GitHub for real provisioning; the environment lifecycle workflow keeps the same interface.

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
feature-environment-feature-environment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/featureenvironment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfigureDnsWorker.java
│       ├── DeployBranchWorker.java
│       ├── NotifyWorker.java
│       └── ProvisionWorker.java
└── src/test/java/featureenvironment/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
