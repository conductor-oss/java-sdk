# Service Mesh Orchestration in Java with Conductor

Orchestrates service mesh configuration: deploy sidecar proxies, configure mTLS, set traffic policies, and validate connectivity. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Onboarding a service into a service mesh requires deploying a sidecar proxy (e.g., Envoy), configuring mutual TLS for encrypted service-to-service communication, setting traffic policies (retries, timeouts, circuit breaking), and validating end-to-end connectivity. Each step depends on the previous one. MTLS cannot be configured until the sidecar is deployed.

Without orchestration, mesh setup is a series of kubectl apply commands in a runbook. If the mTLS step fails, the traffic policy may still be applied, leaving the service in a half-configured state. There is no audit trail of which services are fully mesh-enabled.

## The Solution

**You just write the sidecar-deploy, mTLS-config, traffic-policy, and validation workers. Conductor handles ordered mesh setup, crash-safe resume between sidecar deploy and mTLS configuration, and a full audit of mesh enrollment.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers onboard a service into the mesh: DeploySidecarWorker injects the proxy, ConfigureMtlsWorker sets up mutual TLS, SetTrafficPolicyWorker applies retry and timeout rules, and ValidateWorker confirms end-to-end connectivity.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureMtlsWorker** | `mesh_configure_mtls` | Configures mutual TLS for a service. |
| **DeploySidecarWorker** | `mesh_deploy_sidecar` | Deploys a sidecar proxy for a service in a given namespace. |
| **SetTrafficPolicyWorker** | `mesh_set_traffic_policy` | Sets traffic policy for a service mesh. |
| **ValidateWorker** | `mesh_validate` | Validates connectivity after mesh configuration. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mesh_deploy_sidecar
    │
    ▼
mesh_configure_mtls
    │
    ▼
mesh_set_traffic_policy
    │
    ▼
mesh_validate
```

## Example Output

```
=== Service Mesh Orchestration Demo ===

Step 1: Registering task definitions...
  Registered: mesh_deploy_sidecar, mesh_configure_mtls, mesh_set_traffic_policy, mesh_validate

Step 2: Registering workflow 'service_mesh_orchestration'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mTLS] Configuring mutual TLS for
  [sidecar] Deploying proxy for
  [policy] Setting traffic policy:
  [validate] Connectivity check passed

  Status: COMPLETED
  Output: {enabled=..., certExpiry=..., sidecarId=..., version=...}

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
java -jar target/service-mesh-orchestration-1.0.0.jar
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
java -jar target/service-mesh-orchestration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_mesh_orchestration \
  --version 1 \
  --input '{"serviceName": "sample-name", "payment-service": "sample-payment-service", "namespace": "sample-name", "production": "sample-production", "meshType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_mesh_orchestration -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real Istio or Linkerd control plane, Envoy sidecar injector, and certificate authority, the deploy-mTLS-policy-validate mesh setup workflow stays exactly the same.

- **ConfigureMtlsWorker** (`mesh_configure_mtls`): configure Istio PeerAuthentication or Linkerd identity for real mTLS certificate management
- **DeploySidecarWorker** (`mesh_deploy_sidecar`): use the Istio sidecar injector or manually deploy an Envoy proxy via Kubernetes API
- **SetTrafficPolicyWorker** (`mesh_set_traffic_policy`): apply Istio DestinationRule or Linkerd ServiceProfile for real traffic policies

Replacing Istio with Linkerd behind the configuration workers leaves the mesh onboarding pipeline intact.

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
service-mesh-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/servicemeshorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceMeshOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfigureMtlsWorker.java
│       ├── DeploySidecarWorker.java
│       ├── SetTrafficPolicyWorker.java
│       └── ValidateWorker.java
└── src/test/java/servicemeshorchestration/workers/
    ├── ConfigureMtlsWorkerTest.java        # 7 tests
    ├── DeploySidecarWorkerTest.java        # 8 tests
    ├── SetTrafficPolicyWorkerTest.java        # 7 tests
    └── ValidateWorkerTest.java        # 8 tests
```
