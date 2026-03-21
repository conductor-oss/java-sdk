# Loyalty Program in Java Using Conductor :  Earn Points, Check Tier, Upgrade, Reward

Loyalty program: earn points, check tier, upgrade, deliver rewards. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Loyalty Programs Drive Repeat Purchases When They Work Right

A customer spends $85 and should earn 85 points (1 point per dollar). Their total reaches 950 points, putting them past the 900-point Gold tier threshold. They should be upgraded to Gold and receive the Gold welcome reward (10% off next purchase + free shipping for 30 days). Getting any of these steps wrong .  miscounted points, missed upgrade, wrong reward ,  erodes trust in the program.

The loyalty pipeline must be sequential: points are earned before the tier check, the tier check happens before the upgrade, and the upgrade happens before the reward. If the upgrade step fails, the points should still be credited .  the customer earned them regardless. And every loyalty interaction needs tracking for program analytics: points earned, tiers achieved, rewards delivered, and redemption rates.

## The Solution

**You just write the points calculation, tier evaluation, upgrade, and reward delivery logic. Conductor handles tier evaluation sequencing, reward delivery retries, and points ledger audit trails.**

`EarnPointsWorker` calculates and credits points based on the purchase amount and any multipliers (double points on certain categories, bonus points during promotions). `CheckTierWorker` compares the customer's updated point total against tier thresholds (Silver at 500, Gold at 900, Platinum at 2000) and determines if an upgrade is warranted. `UpgradeTierWorker` processes the tier change .  updating the customer's tier, setting the upgrade date, and triggering welcome communications. `RewardWorker` delivers tier-appropriate rewards ,  discount codes, free shipping, early access to sales, or birthday bonuses. Conductor records every loyalty interaction for program analytics and ROI tracking.

### What You Write: Workers

Points calculation, tier evaluation, upgrade processing, and reward delivery workers each manage one facet of the loyalty lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CheckTierWorker** | `loy_check_tier` | Checks the tier |
| **EarnPointsWorker** | `loy_earn_points` | Awards points |
| **RewardWorker** | `loy_reward` | Distributes rewards and returns reward, tier |
| **UpgradeTierWorker** | `loy_upgrade_tier` | Evaluates upgrade eligibility the tier |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
loy_earn_points
    │
    ▼
loy_check_tier
    │
    ▼
loy_upgrade_tier
    │
    ▼
loy_reward

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
java -jar target/loyalty-program-1.0.0.jar

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
java -jar target/loyalty-program-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow loyalty_program_workflow \
  --version 1 \
  --input '{"customerId": "TEST-001", "purchaseAmount": 100, "currentTier": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w loyalty_program_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real loyalty platform. Smile.io for points, your CRM for tier tracking, Klaviyo for reward emails, and the workflow runs identically in production.

- **EarnPointsWorker** (`loy_earn_points`): integrate with Shopify, Square, or Stripe to calculate points from real transactions, with category-based multipliers and promotional bonus rules
- **UpgradeTierWorker** (`loy_upgrade_tier`): update customer profiles in Salesforce, HubSpot, or a loyalty platform (Smile.io, LoyaltyLion) and trigger welcome email sequences via Klaviyo
- **RewardWorker** (`loy_reward`): generate unique discount codes via Shopify Discount API, activate free shipping rules, and send personalized reward notifications via push, email, or SMS

Modify point multipliers or tier thresholds and the loyalty flow adjusts without touching other workers.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
loyalty-program/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/loyaltyprogram/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LoyaltyProgramExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckTierWorker.java
│       ├── EarnPointsWorker.java
│       ├── RewardWorker.java
│       └── UpgradeTierWorker.java
└── src/test/java/loyaltyprogram/workers/
    ├── CheckTierWorkerTest.java        # 3 tests
    ├── EarnPointsWorkerTest.java        # 3 tests
    └── RewardWorkerTest.java        # 2 tests

```
