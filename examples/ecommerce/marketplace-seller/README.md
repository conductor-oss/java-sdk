# Marketplace Seller Onboarding in Java Using Conductor :  Onboard, Verify, List Products, Manage Orders

Marketplace seller onboarding: register, verify, list products, manage orders. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## Marketplace Seller Onboarding Has Compliance Requirements

A new seller wants to sell on your marketplace. Before their products go live, you need to verify their identity (government ID, business registration), validate their business (tax ID verification, bank account confirmation), list their products with proper categorization and pricing, and enable order management (so they receive orders, manage fulfillment, and handle returns).

Each step has dependencies: products can't be listed until the seller is verified, and order management can't be enabled until products are listed. Identity verification may require manual review (flagged documents), adding variable completion time. If product listing fails (invalid images, missing required fields), the seller's verification is still valid .  you just need to retry the listing step.

## The Solution

**You just write the seller registration, verification, product listing, and order management logic. Conductor handles verification retries, onboarding step sequencing, and seller lifecycle tracking.**

`OnboardSellerWorker` registers the seller with business name, category, contact information, and bank details. `VerifySellerWorker` validates the seller's identity and business legitimacy .  checking government ID, tax registration, bank account ownership, and business licensing. `ListProductsWorker` processes the seller's product catalog ,  validating images, descriptions, pricing, and categorization, and publishing approved listings. `ManageOrdersWorker` enables the seller's order management capabilities ,  order notifications, fulfillment tracking, and return handling. Conductor sequences these steps and records the complete onboarding journey for compliance.

### What You Write: Workers

Seller registration, verification, product listing, and order management workers handle each onboarding phase so marketplace operations stay modular.

| Worker | Task | What It Does |
|---|---|---|
| **ListProductsWorker** | `mkt_list_products` | Listing products for store |
| **ManageOrdersWorker** | `mkt_manage_orders` | Order management activated for store |
| **OnboardSellerWorker** | `mkt_onboard_seller` | Registering seller |
| **VerifySellerWorker** | `mkt_verify_seller` | Verifying documents for seller |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
mkt_onboard_seller
    │
    ▼
mkt_verify_seller
    │
    ▼
mkt_list_products
    │
    ▼
mkt_manage_orders
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
java -jar target/marketplace-seller-1.0.0.jar
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
java -jar target/marketplace-seller-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow marketplace_seller_workflow \
  --version 1 \
  --input '{"sellerId": "TEST-001", "businessName": "test", "category": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w marketplace_seller_workflow -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real seller services. Stripe Connect for identity verification, your product validation pipeline, ShipStation for fulfillment, and the workflow runs identically in production.

- **VerifySellerWorker** (`mkt_verify_seller`): integrate with Stripe Connect for identity verification and bank account validation, or use Jumio/Onfido for document verification with fraud detection
- **ListProductsWorker** (`mkt_list_products`): validate products against marketplace policies using image recognition (Google Vision API for prohibited content), automated pricing checks, and category classification
- **ManageOrdersWorker** (`mkt_manage_orders`): set up real-time order notifications via webhooks, integrate with shipping carriers (EasyPost, ShipStation) for label generation, and configure return/refund policies

Integrate a new verification provider or listing platform and the onboarding flow keeps working.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
marketplace-seller/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/marketplaceseller/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MarketplaceSellerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ListProductsWorker.java
│       ├── ManageOrdersWorker.java
│       ├── OnboardSellerWorker.java
│       └── VerifySellerWorker.java
└── src/test/java/marketplaceseller/workers/
    ├── ListProductsWorkerTest.java        # 2 tests
    ├── ManageOrdersWorkerTest.java        # 2 tests
    ├── OnboardSellerWorkerTest.java        # 2 tests
    └── VerifySellerWorkerTest.java        # 2 tests
```
