# Deployment AI in Java with Conductor :  Risk-Aware Deployment with Change Analysis and Strategy Selection

A Java Conductor workflow that makes deployment decisions intelligently. analyzing code changes in a new version, predicting the deployment risk level, recommending a deployment strategy (blue-green, canary, rolling, or direct), and executing the deployment accordingly. Given a `serviceName`, `version`, and target `environment`, the pipeline produces a change count, risk assessment, chosen strategy, and deployment status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the analyze-predict-recommend-deploy pipeline.

## Deploying with Confidence Instead of Guesswork

Every deployment carries risk. A version with 200 changed files across critical modules needs a different deployment strategy than a version with 3 changed config lines. Teams often pick a strategy based on gut feeling or always use the same approach regardless of risk. This leads to either over-cautious deployments (canary for trivial changes, wasting time) or reckless ones (direct push for risky changes, causing outages).

This workflow makes deployment strategy data-driven. The change analyzer examines what changed between versions. file count, modules affected, breaking changes. The risk predictor evaluates the change analysis and assigns a risk level. The strategy recommender selects the appropriate deployment method based on risk and target environment (production gets more cautious strategies than staging). The deploy executor carries out the chosen strategy. Each step's output informs the next,  risk levels drive strategy selection, and strategy drives execution parameters.

## The Solution

**You just write the change-analysis, risk-prediction, strategy-recommendation, and deployment workers. Conductor handles the decision chain and execution sequencing.**

Four workers form the deployment pipeline. change analysis, risk prediction, strategy recommendation, and deployment execution. The change analyzer examines the diff between versions. The risk predictor scores the changes. The strategy recommender maps risk level and environment to a deployment approach. The executor deploys using the recommended strategy. Conductor sequences the decision chain and ensures the deployment strategy is always backed by data.

### What You Write: Workers

AnalyzeChangesWorker examines the diff between versions, PredictRiskWorker scores deployment risk, RecommendStrategyWorker selects blue-green/canary/rolling, and ExecuteDeployWorker carries out the deployment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeChangesWorker** | `dai_analyze_changes` | Examines the diff between versions: file count, affected modules, database migrations, and breaking changes. |
| **ExecuteDeployWorker** | `dai_execute_deploy` | Deploys the service version using the recommended strategy (blue-green, canary, rolling, or direct). |
| **PredictRiskWorker** | `dai_predict_risk` | Scores deployment risk based on the change analysis (low, medium, high, critical). |
| **RecommendStrategyWorker** | `dai_recommend_strategy` | Selects the deployment strategy (blue-green, canary, rolling, direct) based on risk level and target environment. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
dai_analyze_changes
    │
    ▼
dai_predict_risk
    │
    ▼
dai_recommend_strategy
    │
    ▼
dai_execute_deploy

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
java -jar target/deployment-ai-1.0.0.jar

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
java -jar target/deployment-ai-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dai_deployment_ai \
  --version 1 \
  --input '{"serviceName": "test", "version": "1.0", "environment": "staging"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dai_deployment_ai -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deployment decision step. connect your CI/CD platform (Spinnaker, Argo CD, AWS CodeDeploy) for execution and your observability stack (Datadog, New Relic) for risk assessment, and the deployment workflow stays the same.

- **AnalyzeChangesWorker** (`dai_analyze_changes`): connect to GitHub/GitLab API to diff real versions and detect breaking changes
- **ExecuteDeployWorker** (`dai_execute_deploy`): integrate with Kubernetes, Argo CD, or Spinnaker to execute real deployments
- **PredictRiskWorker** (`dai_predict_risk`): train an ML model on historical deployment outcomes to predict risk more accurately

Replace the simulated deployer with ArgoCD or AWS CodeDeploy and the risk-aware deployment decision chain remains the same.

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
deployment-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/deploymentai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeploymentAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeChangesWorker.java
│       ├── ExecuteDeployWorker.java
│       ├── PredictRiskWorker.java
│       └── RecommendStrategyWorker.java
└── src/test/java/deploymentai/workers/
    ├── AnalyzeChangesWorkerTest.java        # 2 tests
    ├── ExecuteDeployWorkerTest.java        # 2 tests
    ├── PredictRiskWorkerTest.java        # 2 tests
    └── RecommendStrategyWorkerTest.java        # 2 tests

```
