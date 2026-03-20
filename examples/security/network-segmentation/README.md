# Implementing Network Segmentation in Java with Conductor :  Zone Definition, Rule Configuration, Policy Application, and Isolation Verification

A Java Conductor workflow example for network segmentation .  defining network zones (DMZ, internal, restricted), configuring firewall rules between zones, applying security policies, and verifying that isolation is enforced correctly.

## The Problem

You need to segment your network into security zones .  the DMZ for public-facing services, internal zones for application servers, and restricted zones for databases and sensitive systems. Each zone needs firewall rules defining what traffic is allowed between zones, policies must be applied to enforce the rules, and isolation must be verified to ensure no unauthorized cross-zone traffic is possible.

Without orchestration, network segmentation is configured manually in firewall consoles. Rules are added ad hoc, verification is done by security auditors on quarterly schedules, and rule drift (unauthorized changes) goes undetected. A misconfigured rule can expose your database to the internet.

## The Solution

**You just write the firewall rules and zone definitions. Conductor handles ordered deployment of zones and rules, retries on firewall API failures, and a full record of every rule applied and isolation test result.**

Each segmentation step is an independent worker .  zone definition, rule configuration, policy application, and isolation verification. Conductor runs them in sequence: define zones, configure rules between them, apply the policies, then verify isolation. Every segmentation operation is tracked with zone configurations, rules applied, and verification results. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage segmentation end-to-end: DefineZonesWorker establishes network boundaries, ConfigureRulesWorker sets inter-zone firewall policies, ApplyPoliciesWorker deploys security groups, and VerifyIsolationWorker confirms no unauthorized cross-zone traffic.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyPoliciesWorker** | `ns_apply_policies` | Applies security group policies across the VPC to enforce zone boundaries |
| **ConfigureRulesWorker** | `ns_configure_rules` | Configures firewall rules between zones (e.g., 28 rules between DMZ, app, data, and mgmt) |
| **DefineZonesWorker** | `ns_define_zones` | Defines network security zones (e.g., DMZ, application, data, management) |
| **VerifyIsolationWorker** | `ns_verify_isolation` | Verifies zone isolation by confirming no unauthorized cross-zone traffic exists |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ns_define_zones
    │
    ▼
ns_configure_rules
    │
    ▼
ns_apply_policies
    │
    ▼
ns_verify_isolation
```

## Example Output

```
=== Example 600: Network Segmentatio ===

Step 1: Registering task definitions...
  Registered: ns_define_zones, ns_configure_rules, ns_apply_policies, ns_verify_isolation

Step 2: Registering workflow 'network_segmentation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apply] Security group policies applied across VPC
  [rules] Configured 28 firewall rules between zones
  [zones] Defined 4 zones: DMZ, app, data, mgmt
  [verify] Zone isolation verified: no unauthorized cross-zone traffic

  Status: COMPLETED

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
java -jar target/network-segmentation-1.0.0.jar
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
java -jar target/network-segmentation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow network_segmentation_workflow \
  --version 1 \
  --input '{"environment": "sample-environment", "production": "sample-production", "segmentationType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w network_segmentation_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker manages one segmentation step .  connect DefineZonesWorker to your SDN controller (NSX, Calico), ConfigureRulesWorker to AWS Security Groups or Palo Alto, and the zone-configure-verify workflow stays the same.

- **ApplyPoliciesWorker** (`ns_apply_policies`): deploy policies via infrastructure-as-code (Terraform, Pulumi) or push to network policy engines (Calico, Cilium)
- **ConfigureRulesWorker** (`ns_configure_rules`): create firewall rules in AWS Security Groups, Azure NSGs, GCP Firewall, or on-prem firewalls (Palo Alto, Fortinet)
- **DefineZonesWorker** (`ns_define_zones`): define network zones in your SDN controller (NSX, Calico, AWS VPC), mapping IP ranges and service groups to zones

Swap in your SDN controller or cloud firewall API and the zone-configure-verify orchestration persists unmodified.

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
network-segmentation-network-segmentation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/networksegmentation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyPoliciesWorker.java
│       ├── ConfigureRulesWorker.java
│       ├── DefineZonesWorker.java
│       └── VerifyIsolationWorker.java
└── src/test/java/networksegmentation/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
