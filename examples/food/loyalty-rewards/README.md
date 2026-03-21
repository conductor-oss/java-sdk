# Loyalty Rewards in Java with Conductor

Processes restaurant loyalty rewards: calculating points earned, evaluating tier status, applying redemptions, and updating the customer's loyalty account. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process loyalty rewards for a restaurant customer. The workflow calculates points earned from the current order, checks the customer's loyalty tier (bronze, silver, gold, platinum), processes any point redemptions the customer wants to apply, and updates their loyalty account. Earning points without checking the tier means missing tier-specific bonuses; redeeming without validating the balance allows overdrafts.

Without orchestration, you'd embed loyalty logic in the checkout flow. manually calculating points, querying tier status, validating redemption amounts, and updating balances in a single transaction, hoping the database update does not fail after the redemption has already been applied to the order.

## The Solution

**You just write the points calculation, tier evaluation, redemption processing, and account update logic. Conductor handles points calculation retries, tier evaluation, and reward redemption audit trails.**

Each loyalty concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (earn points, check tier, redeem, track), retrying if the loyalty database is unavailable, tracking every loyalty transaction, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Purchase tracking, points calculation, tier evaluation, and reward redemption workers each manage one dimension of the restaurant loyalty program.

| Worker | Task | What It Does |
|---|---|---|
| **CheckTierWorker** | `lyr_check_tier` | Evaluates the customer's loyalty tier (Silver, Gold, or Platinum) based on total points |
| **EarnPointsWorker** | `lyr_earn_points` | Calculates points earned from the order total and adds them to the customer's balance |
| **RedeemWorker** | `lyr_redeem` | Redeems the requested points at the customer's tier, calculates the discount, and deducts from their balance |
| **TrackWorker** | `lyr_track` | Updates the customer's loyalty account with earned and redeemed points and records the transaction |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
lyr_earn_points
    │
    ▼
lyr_check_tier
    │
    ▼
lyr_redeem
    │
    ▼
lyr_track

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
java -jar target/loyalty-rewards-1.0.0.jar

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
java -jar target/loyalty-rewards-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow loyalty_rewards_737 \
  --version 1 \
  --input '{"customerId": "TEST-001", "orderTotal": "sample-orderTotal", "redeemPoints": "sample-redeemPoints"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w loyalty_rewards_737 -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real loyalty platform. your POS for purchase data, your rewards engine for point calculations, your customer app for tier updates and redemptions, and the workflow runs identically in production.

- **Points earner**: calculate points using tier-specific multipliers and promotional bonus rules from your loyalty platform (LevelUp, Thanx, Punchh)
- **Tier checker**: evaluate tier status based on cumulative spending or visit frequency; trigger tier upgrade notifications
- **Redemption processor**: validate point balance, apply discounts to the order, and deduct points from the loyalty account
- **Account tracker**: update the customer's loyalty dashboard with transaction history, current balance, and progress toward the next tier

Adjust point values or tier criteria and the rewards pipeline processes them with no structural changes.

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
loyalty-rewards/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/loyaltyrewards/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LoyaltyRewardsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckTierWorker.java
│       ├── EarnPointsWorker.java
│       ├── RedeemWorker.java
│       └── TrackWorker.java
└── src/test/java/loyaltyrewards/workers/
    ├── EarnPointsWorkerTest.java
    └── TrackWorkerTest.java

```
