# Load Balancer Configuration in Java with Conductor :  Discover Backends, Configure Rules, Apply, Health Check

Load balancer configuration workflow: discover backends, configure rules, apply config, and health check. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## Load Balancer Changes Need Discovery, Rules, and Verification

A new version of the API service is deployed to 3 instances. The load balancer needs to know about these instances (their IPs and ports), have the routing rules updated (path-based routing for /api/v2, header-based routing for canary traffic, weighted distribution across versions), apply the new configuration without dropping existing connections, and verify that health checks pass for all backends.

Applying a load balancer configuration change without verification is dangerous .  a misconfigured rule can blackhole traffic, and a bad backend can cause cascading failures. The discovery step ensures the load balancer knows about all healthy backends. The rule configuration step defines how traffic is distributed. The apply step makes the changes live. The health check step confirms everything works end-to-end.

## The Solution

**You write the backend discovery and routing rules. Conductor handles the discover-configure-apply-verify sequence and records every configuration change.**

`DiscoverBackendsWorker` finds all available backend instances for the service .  querying service discovery, Kubernetes endpoints, or cloud provider APIs for healthy instances with their connection details. `ConfigureRulesWorker` builds the routing configuration ,  path-based rules, host-based rules, weighted distribution, sticky sessions, and health check parameters. `ApplyConfigWorker` applies the configuration to the load balancer with graceful connection draining for removed backends. `HealthCheckWorker` verifies all backends are receiving traffic and responding correctly ,  checking response codes, latency, and error rates post-configuration. Conductor records every configuration change for audit.

### What You Write: Workers

Four workers configure the load balancer. Discovering backends, building routing rules, applying the config, and verifying health checks.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyConfigWorker** | `lb_apply_config` | Applies the load balancer configuration without dropping connections. |
| **ConfigureRulesWorker** | `lb_configure_rules` | Configures routing rules for the load balancer. |
| **DiscoverBackendsWorker** | `lb_discover_backends` | Discovers backend servers for the load balancer. |
| **HealthCheckWorker** | `lb_health_check` | Performs health check on all backends after config change. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
lb_discover_backends
    │
    ▼
lb_configure_rules
    │
    ▼
lb_apply_config
    │
    ▼
lb_health_check
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
java -jar target/load-balancer-config-1.0.0.jar
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
java -jar target/load-balancer-config-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow load_balancer_config_workflow \
  --version 1 \
  --input '{"loadBalancer": "test-value", "action": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w load_balancer_config_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one load balancer concern .  replace the simulated calls with AWS ALB APIs, NGINX Plus configuration, or HAProxy Data Plane API, and the configuration workflow runs unchanged.

- **DiscoverBackendsWorker** (`lb_discover_backends`): query Kubernetes Service endpoints, Consul service catalog, or AWS EC2 describe-instances for real backend discovery
- **ConfigureRulesWorker** (`lb_configure_rules`): define path-based routing rules, header-based routing, weighted traffic splitting, or sticky sessions based on the service topology and deployment strategy
- **ApplyConfigWorker** (`lb_apply_config`): update AWS ALB/NLB via the ELB API, configure Nginx via the Plus API, or update HAProxy via the Data Plane API with zero-downtime reloads
- **HealthCheckWorker** (`lb_health_check`): verify backends via HTTP health checks with expected status codes, check load balancer metrics in CloudWatch or Prometheus, and alert on unhealthy targets

Plug in AWS ALB or Nginx APIs; the configuration workflow preserves the same discover-configure-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
load-balancer-config/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/loadbalancerconfig/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LoadBalancerConfigExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyConfigWorker.java
│       ├── ConfigureRulesWorker.java
│       ├── DiscoverBackendsWorker.java
│       └── HealthCheckWorker.java
└── src/test/java/loadbalancerconfig/workers/
    ├── ApplyConfigWorkerTest.java        # 4 tests
    ├── ConfigureRulesWorkerTest.java        # 4 tests
    ├── DiscoverBackendsWorkerTest.java        # 5 tests
    └── HealthCheckWorkerTest.java        # 4 tests
```
