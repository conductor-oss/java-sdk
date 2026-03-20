# Network Automation in Java with Conductor :  Plan Changes, Apply Config, Audit, Verify Connectivity

Automates network infrastructure changes using [Conductor](https://github.com/conductor-oss/conductor). This workflow audits the current network state (devices, configurations, topology), plans the required configuration changes, applies them to network devices, and verifies connectivity is intact after the changes. You write the network logic, Conductor handles retries, failure routing, durability, and observability for free.

## Network Changes Without Outages

You need to update firewall rules across 12 switches to allow traffic from a new subnet. Doing this manually means SSHing into each device, running show commands to understand the current state, typing configuration commands, and hoping you do not fat-finger a rule that blocks production traffic. The safe approach: audit all devices first to understand the current configuration, plan the exact changes needed, apply them systematically, and verify connectivity end-to-end before declaring success.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the device configuration and verification logic. Conductor handles audit-plan-apply-verify sequencing and records every network change for compliance.**

`PlanChangesWorker` determines which devices need configuration changes and generates the specific commands for each device type (Cisco IOS, Juniper JunOS, Palo Alto). `ApplyConfigWorker` pushes the configuration changes to each network device using programmatic access. SSH, NETCONF, or vendor APIs. `AuditNetworkWorker` compares the actual device configurations against the desired state, identifying any deviations or incomplete changes. `VerifyConnectivityWorker` tests end-to-end connectivity through the network .  pinging through firewalls, testing port access, and validating routing. Conductor sequences these steps and records every configuration change for network audit.

### What You Write: Workers

Four workers automate network changes. Planning configurations, applying to devices, auditing the network state, and verifying end-to-end connectivity.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyConfig** | `na_apply_config` | Applies planned configuration to network devices. |
| **AuditNetwork** | `na_audit` | Audits network infrastructure to discover devices and current configuration. |
| **PlanChanges** | `na_plan_changes` | Plans network configuration changes based on the audit. |
| **VerifyConnectivity** | `na_verify_connectivity` | Verifies network connectivity after configuration changes. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> ApplyConfig -> AuditNetwork -> PlanChanges -> VerifyConnectivity -> Output
```

## Example Output

```
=== Network Automation Demo: Automated Network Configuration Management ===

Step 1: Registering task definitions...
  Registered: na_apply_config, na_audit, na_plan_changes, na_verify_connectivity

Step 2: Registering workflow 'network_automation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [na_apply_config] Configuration pushed to 12 devices (planned:
  [na_audit] Network
  [na_plan_changes]
  [na_verify_connectivity] All connectivity tests passed for

  Status: COMPLETED
  Output: {applied=..., devicesConfigured=..., processed=..., auditId=...}

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
java -jar target/network-automation-1.0.0.jar
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
java -jar target/network-automation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow network_automation \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w network_automation -s COMPLETED -c 5
```

## How to Extend

Each worker handles one network operation .  replace the simulated calls with Netmiko, NAPALM, or Ansible Network modules for real multi-vendor device configuration, and the automation workflow runs unchanged.

- **PlanChanges** (`na_plan_changes`): generate a network change plan by diffing desired configuration against current device state, identifying which routers, switches, or firewalls need updates
- **ApplyConfig** (`na_apply_config`): use Netmiko or Napalm (via subprocess) for multi-vendor device configuration, Ansible Network modules for playbook-based automation, or Terraform with the relevant network provider
- **AuditNetwork** (`na_audit_network`): compare running configs against desired state using Batfish for network verification, or NAPALM's `compare_config()` for per-device compliance checking
- **VerifyConnectivity** (`na_verify_connectivity`): run automated connectivity tests using Iperf for throughput, traceroute for path verification, and port scanning for firewall rule validation

Replace with NETCONF or vendor APIs for real device configuration; the network automation pipeline preserves the same audit interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
network-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/networkautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NetworkAutomationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyConfig.java
│       ├── AuditNetwork.java
│       ├── PlanChanges.java
│       └── VerifyConnectivity.java
└── src/test/java/networkautomation/workers/
    ├── ApplyConfigTest.java        # 8 tests
    ├── AuditNetworkTest.java        # 8 tests
    ├── PlanChangesTest.java        # 8 tests
    └── VerifyConnectivityTest.java        # 8 tests
```
