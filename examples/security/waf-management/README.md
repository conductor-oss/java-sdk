# Implementing WAF Management in Java with Conductor :  Traffic Analysis, Rule Updates, Deployment, and Protection Verification

A Java Conductor workflow example for Web Application Firewall (WAF) management .  analyzing traffic patterns, updating WAF rules for detected threats, deploying rule changes, and verifying that protection is active without blocking legitimate traffic.

## The Problem

Your WAF protects web applications from attacks. SQL injection, XSS, bot traffic. But WAF rules must evolve with threats. Traffic must be analyzed for new attack patterns, rules must be updated (add blocks for new attack signatures, whitelist legitimate traffic), changes must be deployed to the WAF, and protection must be verified (attacks are blocked, legitimate traffic passes).

Without orchestration, WAF management is reactive .  a security engineer manually reviews WAF logs, writes rules in the vendor console, and deploys without testing. Overly aggressive rules block legitimate customers, overly permissive rules let attacks through, and there's no automated feedback loop.

## The Solution

**You just write the traffic analysis and rule deployment logic. Conductor handles the rule update sequence, retries on CDN deployment failures, and a full change log of every rule modification and verification result.**

Each WAF step is an independent worker .  traffic analysis, rule updates, deployment, and verification. Conductor runs them in sequence: analyze traffic for threats, update rules accordingly, deploy to the WAF, then verify protection. Every rule change is tracked with the threat that triggered it, the rule modification, and verification results. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The WAF pipeline uses AnalyzeTrafficWorker to detect attack patterns, UpdateRulesWorker to generate blocking rules, DeployRulesWorker to push changes to the CDN, and VerifyProtectionWorker to confirm attacks are blocked without affecting legitimate users.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrafficWorker** | `waf_analyze_traffic` | Analyzes web traffic patterns to detect attack signatures (e.g., SQL injection attempts) |
| **DeployRulesWorker** | `waf_deploy_rules` | Deploys updated WAF rules to the CDN/WAF provider (e.g., CloudFront WAF) |
| **UpdateRulesWorker** | `waf_update_rules` | Generates new blocking rules based on detected attack patterns |
| **VerifyProtectionWorker** | `waf_verify_protection` | Runs attack simulations to verify that the deployed rules block the detected threats |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
waf_analyze_traffic
    │
    ▼
waf_update_rules
    │
    ▼
waf_deploy_rules
    │
    ▼
waf_verify_protection
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
java -jar target/waf-management-1.0.0.jar
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
java -jar target/waf-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow waf_management_workflow \
  --version 1 \
  --input '{"application": "test-value", "threatType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w waf_management_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker owns one WAF operation .  connect AnalyzeTrafficWorker to AWS WAF or Cloudflare logs, DeployRulesWorker to your WAF provider's API, and the analyze-update-deploy-verify workflow stays the same.

- **AnalyzeTrafficWorker** (`waf_analyze_traffic`): analyze real WAF logs from AWS WAF, Cloudflare, or Akamai for attack patterns and false positives
- **DeployRulesWorker** (`waf_deploy_rules`): deploy rule changes to production WAF via AWS WAF API, Cloudflare API, or Akamai Property Manager
- **UpdateRulesWorker** (`waf_update_rules`): modify WAF rules using the provider's API .  add block rules for new attack signatures, whitelist known-good patterns

Connect to your WAF provider's API and the analyze-update-deploy-verify cycle continues with no orchestration changes.

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
waf-management-waf-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/wafmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrafficWorker.java
│       ├── DeployRulesWorker.java
│       ├── UpdateRulesWorker.java
│       └── VerifyProtectionWorker.java
└── src/test/java/wafmanagement/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
