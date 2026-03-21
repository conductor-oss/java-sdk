# Service Orchestration in Java with Conductor

Orchestrate auth, catalog, cart, and checkout microservices. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A typical e-commerce purchase flow spans four microservices: authenticate the user, look up the product in the catalog, add it to the shopping cart, and process checkout. Each step depends on the output of the previous one, the catalog lookup needs an auth token, the cart needs the product details, and checkout needs the cart total.

Without orchestration, the frontend or a BFF layer chains four HTTP calls with manual error handling. If the catalog service is slow, the entire request hangs with no timeout isolation. There is no single place to see the full purchase flow or retry a failed step.

## The Solution

**You just write the authentication, catalog-lookup, cart, and checkout workers. Conductor handles auth-to-checkout sequencing, per-step timeout isolation, and complete purchase flow traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers drive the purchase flow: AuthenticateWorker validates user credentials, CatalogLookupWorker retrieves product details, AddToCartWorker manages the shopping cart, and CheckoutWorker processes the final payment.

| Worker | Task | What It Does |
|---|---|---|
| **AddToCartWorker** | `so_add_to_cart` | Adds items to a shopping cart. |
| **AuthenticateWorker** | `so_authenticate` | Authenticates a user and returns a JWT token. |
| **CatalogLookupWorker** | `so_catalog_lookup` | Looks up a product in the catalog. |
| **CheckoutWorker** | `so_checkout` | Processes checkout for a cart. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
so_authenticate
    │
    ▼
so_catalog_lookup
    │
    ▼
so_add_to_cart
    │
    ▼
so_checkout

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
java -jar target/service-orchestration-1.0.0.jar

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
java -jar target/service-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_orchestration_291 \
  --version 1 \
  --input '{"userId": "TEST-001", "productId": "TEST-001", "quantity": "sample-quantity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_orchestration_291 -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real identity provider (Auth0, Cognito), product catalog, shopping cart (Redis, DynamoDB), and payment gateway, the auth-catalog-cart-checkout workflow stays exactly the same.

- **AddToCartWorker** (`so_add_to_cart`): call your cart microservice or write directly to the shopping cart database (Redis, DynamoDB)
- **AuthenticateWorker** (`so_authenticate`): validate credentials against your identity provider (Auth0, Keycloak, AWS Cognito)
- **CatalogLookupWorker** (`so_catalog_lookup`): query your product catalog database or service API for real product data and pricing

Connecting AuthenticateWorker to Auth0 or CatalogLookupWorker to a real product database preserves the purchase pipeline.

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
service-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/serviceorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AddToCartWorker.java
│       ├── AuthenticateWorker.java
│       ├── CatalogLookupWorker.java
│       └── CheckoutWorker.java
└── src/test/java/serviceorchestration/workers/
    ├── AddToCartWorkerTest.java        # 8 tests
    ├── AuthenticateWorkerTest.java        # 8 tests
    ├── CatalogLookupWorkerTest.java        # 8 tests
    └── CheckoutWorkerTest.java        # 8 tests

```
