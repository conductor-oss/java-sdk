# Service Discovery in Java with Conductor: Register, Health Check, Update Routing, Notify Consumers

You deployed v2.4.1 of the recommendation engine ten minutes ago. It's running, it's healthy, and it's serving exactly zero traffic. because the routing table still points to the three old instances, the load balancer doesn't know it exists, and the five downstream services that depend on it are still hitting stale endpoints. One of those old instances just ran out of memory, so now a third of your recommendation requests are 503ing and nobody can figure out why the "new deploy" didn't fix it. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to automate the full service registration lifecycle, register, health-check, update routing, and notify consumers, so new instances actually receive traffic and dead ones stop getting it.

## New Services That Are Actually Reachable

You just deployed a new instance of the user-api service. But other services cannot find it yet. It is not registered in the service mesh, the load balancer does not know about it, and downstream consumers are still routing to the old set of instances. The workflow registers the instance, verifies it passes health checks, updates routing so traffic reaches it, and notifies consumers that a new endpoint is available.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the registration and health check logic. Conductor handles the register-verify-route-notify sequence and ensures no traffic reaches an unhealthy instance.**

`RegisterWorker` adds the service instance to the discovery registry with its endpoint, version, and metadata, producing a deterministic registration ID. `HealthCheckWorker` runs initial health checks against the registered endpoint. Verifying HTTP readiness and recording response times. `UpdateRoutingWorker` configures load balancers and service meshes to route traffic to the new healthy instance. `NotifyConsumersWorker` informs downstream services of the new endpoint via webhooks, DNS updates, or service mesh configuration refresh. Conductor sequences these steps, ensuring no traffic reaches an unhealthy instance.

### What You Write: Workers

Four workers manage service registration. Registering the instance, running health probes, updating routing tables, and notifying downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| `RegisterWorker` | `sd_register` | Registers service instance in the discovery registry with a deterministic ID, endpoint, and version |
| `HealthCheckWorker` | `sd_health_check` | Probes the registered endpoint for readiness, records response time and health status |
| `UpdateRoutingWorker` | `sd_update_routing` | Updates load balancer and routing tables to include the new healthy instance |
| `NotifyConsumersWorker` | `sd_notify_consumers` | Notifies downstream consumers via webhook that a new service endpoint is available |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls, the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sd_register
    │
    ▼
sd_health_check
    │
    ▼
sd_update_routing
    │
    ▼
sd_notify_consumers
```

## Example Output

```
=== Service Discovery (DevOps) Demo ===

Step 1: Registering task definitions...
  Registered: sd_register, sd_health_check, sd_update_routing, sd_notify_consumers

Step 2: Registering workflow 'service_discovery_devops_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 7a440491-9740-17e3-332a-455b6eb74fa0

[sd_register] Registering service: recommendation-engine v2.4.1 at http://rec-svc.prod.internal:8080
[sd_health_check] Health check on endpoint-value -> HTTP 200 OK (12ms)
[sd_update_routing] Added endpoint-value to routing table for default
[sd_notify_consumers] Notified 5 consumers about default availability update

  Status: COMPLETED
  Output: {registerResult=Success, notify_consumersResult=Success}

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
java -jar target/service-discovery-devops-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== Service Discovery (DevOps) Demo ===

Step 1: Registering task definitions...
  Registered.

Step 2: Registering workflow...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...

[sd_register] Registering service: recommendation-engine v2.4.1 at http://rec-svc.prod.internal:8080
[sd_health_check] Health check on http://rec-svc.prod.internal:8080 -> HTTP 200 OK (12ms)
[sd_update_routing] Added http://rec-svc.prod.internal:8080 to routing table for recommendation-engine
[sd_notify_consumers] Notified 5 consumers about recommendation-engine availability update

  Workflow ID: 3fa85f64-5542-4562-b3fc-2c963f66afa6

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {registerResult={registrationId=reg-recommendation-engine-2.4.1, ...}, notify_consumersResult={notified=true, ...}}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/service-discovery-devops-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_discovery_devops_workflow \
  --version 1 \
  --input '{"serviceName": "recommendation-engine", "endpoint": "http://rec-svc.prod.internal:8080", "version": "2.4.1", "healthCheckUrl": "http://rec-svc.prod.internal:8080/health"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_discovery_devops_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one service discovery step. Replace the simulated calls with Consul, Eureka, or Kubernetes Service APIs for real registration and routing updates, and the discovery workflow runs unchanged.

- **`RegisterWorker`**: Replace the simulated registration with a call to [HashiCorp Consul](https://www.consul.io/) service catalog, [Netflix Eureka](https://github.com/Netflix/eureka) registry, or the [Kubernetes Service API](https://kubernetes.io/docs/concepts/services-networking/service/) to register the instance in a real service mesh.

- **`HealthCheckWorker`**: Replace the simulated health probe with real HTTP or gRPC health checks against the service endpoint, or integrate with [Kubernetes readiness probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) to verify the instance is ready to receive traffic.

- **`UpdateRoutingWorker`**: Replace the simulated routing update with calls to [Istio VirtualService](https://istio.io/latest/docs/reference/config/networking/virtual-service/) configuration, NGINX upstream config updates, or [AWS ALB target group](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-target-groups.html) registration to route real traffic to the new instance.

- **`NotifyConsumersWorker`**: Replace the simulated notification with DNS record updates, webhook callbacks to downstream services, or service mesh sidecar configuration refresh (e.g., Envoy xDS push) so consumers discover the new endpoint.

Integrate with Consul or Eureka for real service registration; the discovery pipeline preserves the same register-verify-route interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
service-discovery-devops/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/servicediscoverydevops/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceDiscoveryDevopsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── RegisterWorker.java      # Registers service in discovery registry
│       ├── HealthCheckWorker.java   # Probes endpoint health and response time
│       ├── UpdateRoutingWorker.java # Updates load balancer routing tables
│       └── NotifyConsumersWorker.java # Notifies downstream consumers
└── src/test/java/servicediscoverydevops/workers/
    ├── RegisterWorkerTest.java      # Tests deterministic registration ID, output fields
    ├── HealthCheckWorkerTest.java   # Tests health status, response time presence
    ├── UpdateRoutingWorkerTest.java # Tests routing update outputs
    └── NotifyConsumersWorkerTest.java # Tests notification count, consumer list
```
