# Auction Workflow in Java Using Conductor :  Open Bidding, Collect Bids, Close, Determine Winner, Settle

Auction workflow: open bidding, collect bids, close, determine winner, settle. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## Auctions Have Strict Lifecycle Rules

An auction for a vintage watch starts at $500. Bids arrive over the next 24 hours. The auction must close at exactly the scheduled time .  not a second early or late. The highest valid bid wins, but only if it meets the reserve price. The winner's payment must be processed, the seller must be notified with the final price, and the item must be marked as sold.

Each stage has specific timing and validation requirements. Bidding can only happen during the open period. Each bid must be validated (is the bidder registered? Does the bid exceed the current highest?). The closing must be deterministic. Winner determination must handle edge cases (tied bids, reserve not met). Settlement must charge the winner and release the item to escrow. If payment fails, the next highest bidder should be contacted.

## The Solution

**You just write the bidding, auction closing, winner determination, and settlement logic. Conductor handles bid timing, settlement retries, and complete auction audit trails.**

`OpenBiddingWorker` initializes the auction with item details, starting price, reserve price, and duration. `CollectBidsWorker` accepts and validates bids during the bidding period .  checking bidder eligibility, bid amount validity, and incrementing the current price. `CloseAuctionWorker` closes bidding at the scheduled time and locks the bid list. `DetermineWinnerWorker` identifies the highest valid bid, checks against the reserve price, and declares the winner. `SettleWorker` processes payment from the winner, notifies all parties, and updates the item status. Conductor sequences these stages and records every bid for auction transparency.

### What You Write: Workers

Bidding, closing, winner determination, and settlement workers operate on auction state independently, letting you modify bidding rules without touching settlement logic.

| Worker | Task | What It Does |
|---|---|---|
| **CloseAuctionWorker** | `auc_close_auction` | Performs the close auction operation |
| **CollectBidsWorker** | `auc_collect_bids` | Performs the collect bids operation |
| **DetermineWinnerWorker** | `auc_determine_winner` | Handles determine winner |
| **OpenBiddingWorker** | `auc_open_bidding` | Performs the open bidding operation |
| **SettleAuctionWorker** | `auc_settle` | Performs the settle auction operation |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
auc_open_bidding
    │
    ▼
auc_collect_bids
    │
    ▼
auc_close_auction
    │
    ▼
auc_determine_winner
    │
    ▼
auc_settle
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
java -jar target/auction-workflow-1.0.0.jar
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
java -jar target/auction-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow auction_workflow \
  --version 1 \
  --input '{"auctionId": "TEST-001", "itemName": "test", "startingPrice": 100}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w auction_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real auction systems .  your bidding engine for real-time bid processing, Stripe for winner payment, your notification service for party alerts, and the workflow runs identically in production.

- **OpenBiddingWorker** (`auc_open_bidding`): publish the auction listing to the marketplace, set the opening price and reserve, and start accepting bids via the bidding API
- **CollectBidsWorker** (`auc_collect_bids`): integrate with a real-time WebSocket server for live bid updates, or use Redis sorted sets for atomic bid comparison and insertion
- **CloseAuctionWorker** (`auc_close_auction`): enforce the auction deadline, implement anti-sniping extensions (extending if a bid arrives in the last 30 seconds), and lock the final bid state
- **DetermineWinnerWorker** (`auc_determine_winner`): implement proxy bidding (automatic bid incrementing up to a maximum), reserve price logic, and highest-bid selection with tie-breaking rules
- **SettleAuctionWorker** (`auc_settle`): process payments via Stripe Payment Intents or PayPal Orders API, with escrow hold until the seller ships and the buyer confirms receipt

Swap the bidding engine or settlement processor and the auction flow continues without modification.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
auction-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/auctionworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AuctionWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseAuctionWorker.java
│       ├── CollectBidsWorker.java
│       ├── DetermineWinnerWorker.java
│       ├── OpenBiddingWorker.java
│       └── SettleAuctionWorker.java
└── src/test/java/auctionworkflow/workers/
    ├── CollectBidsWorkerTest.java        # 2 tests
    ├── DetermineWinnerWorkerTest.java        # 2 tests
    └── SettleAuctionWorkerTest.java        # 2 tests
```
