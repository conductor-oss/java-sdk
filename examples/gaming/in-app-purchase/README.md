# In App Purchase in Java Using Conductor

Processes an in-app purchase: selecting an item from the game catalog, verifying eligibility, charging payment, delivering the virtual item to the player's inventory, and generating a receipt. Uses [Conductor](https://github.

## The Problem

You need to process an in-app purchase in a game. The player selects an item to buy, the purchase is verified with the platform's payment system (ensuring the transaction is legitimate), payment is charged, the virtual item is delivered to the player's inventory, and a receipt is generated. Delivering items without payment verification enables fraud; failing to deliver after payment creates support tickets and chargebacks.

Without orchestration, you'd handle the purchase flow in a single service that validates the item, calls the payment API, grants the item, and generates receipts. manually handling race conditions when a player buys the same item twice, retrying failed item deliveries, and reconciling payment records with inventory grants.

## The Solution

**You just write the item selection, eligibility check, payment processing, item delivery, and receipt generation logic. Conductor handles payment retries, entitlement delivery, and purchase audit trails for every transaction.**

Each purchase concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (select, verify, charge, deliver, receipt), retrying if the payment platform is temporarily unavailable, tracking every purchase with full audit trail, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Product validation, payment processing, entitlement delivery, and receipt generation workers isolate each phase of a microtransaction.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `iap_charge` | Charges the item price to the player's account and returns a transaction ID |
| **DeliverWorker** | `iap_deliver` | Delivers the purchased item to the player's inventory and confirms the update |
| **ReceiptWorker** | `iap_receipt` | Generates a purchase receipt with transaction ID, player ID, and completion status |
| **SelectItemWorker** | `iap_select_item` | Validates the selected item in the game catalog and returns item details (name, type, e.g., Dragon Armor cosmetic) |
| **VerifyWorker** | `iap_verify` | Verifies purchase eligibility, checking account balance and age restrictions |

Workers implement game backend operations. matchmaking, score processing, reward distribution,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
iap_select_item
    │
    ▼
iap_verify
    │
    ▼
iap_charge
    │
    ▼
iap_deliver
    │
    ▼
iap_receipt

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
java -jar target/in-app-purchase-1.0.0.jar

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
java -jar target/in-app-purchase-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow in_app_purchase_744 \
  --version 1 \
  --input '{"playerId": "TEST-001", "itemId": "TEST-001", "price": 100}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w in_app_purchase_744 -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real purchase stack. Apple/Google IAP APIs for payment verification, your game server for item delivery, your receipt system for transaction records, and the workflow runs identically in production.

- **Item selector**: validate the item exists in your game catalog and check purchase eligibility (level requirements, one-time purchases, regional restrictions)
- **Purchase verifier**: verify with Apple App Store (verifyReceipt), Google Play (purchases.products.get), or your custom payment system
- **Payment charger**: process payment via the platform's billing API or your payment gateway (Stripe, Xsolla, PayPal)
- **Item deliverer**: grant the virtual item to the player's inventory in your game database with idempotency to prevent duplicate grants
- **Receipt generator**: create a purchase receipt with transaction ID, item details, and timestamp for the player's purchase history

Switch payment processors or entitlement systems and the purchase flow maintains its structure.

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
in-app-purchase/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/inapppurchase/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InAppPurchaseExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChargeWorker.java
│       ├── DeliverWorker.java
│       ├── ReceiptWorker.java
│       ├── SelectItemWorker.java
│       └── VerifyWorker.java
└── src/test/java/inapppurchase/workers/
    ├── ReceiptWorkerTest.java
    └── SelectItemWorkerTest.java

```
