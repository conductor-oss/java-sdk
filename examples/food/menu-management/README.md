# Menu Management in Java with Conductor

Manages a restaurant menu lifecycle: creating items with descriptions, setting prices, categorizing into sections, publishing to ordering platforms, and syncing updates. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage a restaurant's menu lifecycle. creating menu items with descriptions and photos, setting prices based on food costs and margins, organizing items into categories (appetizers, entrees, desserts), publishing the menu to ordering platforms, and updating items as ingredients or prices change. Publishing a menu with incorrect prices erodes margins; outdated items lead to orders the kitchen cannot fulfill.

Without orchestration, you'd manage menus through a combination of spreadsheets, POS admin panels, and third-party delivery platform dashboards. manually keeping prices synchronized across platforms, updating photos and descriptions in multiple places, and hoping no platform shows a discontinued item.

## The Solution

**You just write the item creation, pricing, categorization, platform publishing, and update syncing logic. Conductor handles pricing retries, publication sequencing, and menu change audit trails.**

Each menu concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (create items, price, categorize, publish, update), retrying if a platform API is unavailable, tracking every menu change, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Item creation, pricing, categorization, and publication workers handle menu updates through discrete, auditable operations.

| Worker | Task | What It Does |
|---|---|---|
| **CategorizeWorker** | `mnu_categorize` | Organizes menu items into categories (entrees, desserts) and returns the categorized menu with item counts |
| **CreateItemsWorker** | `mnu_create_items` | Creates menu items with names and descriptions (e.g., Grilled Salmon, Truffle Pasta, Tiramisu) for the restaurant |
| **PriceWorker** | `mnu_price` | Sets prices for each menu item based on food cost and margin targets |
| **PublishWorker** | `mnu_publish` | Publishes the menu to ordering platforms and returns the menu ID, publish status, and date |
| **UpdateWorker** | `mnu_update` | Confirms the menu is live and synced across all platforms, returning the final menu status and item count |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
mnu_create_items
    │
    ▼
mnu_price
    │
    ▼
mnu_categorize
    │
    ▼
mnu_publish
    │
    ▼
mnu_update

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
java -jar target/menu-management-1.0.0.jar

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
java -jar target/menu-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow menu_management_734 \
  --version 1 \
  --input '{"restaurantId": "TEST-001", "menuName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w menu_management_734 -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real menu systems. your recipe database for item creation, your POS for pricing, your delivery platforms (DoorDash, Uber Eats) for publishing and sync, and the workflow runs identically in production.

- **Item creator**: persist menu items with descriptions, photos, allergen tags, and modifiers to your menu management system
- **Pricing engine**: calculate prices based on food cost percentages, competitor pricing, and demand-based dynamic pricing
- **Categorizer**: organize items into categories and meal dayparts (breakfast, lunch, dinner) with display ordering
- **Publisher**: sync the menu to your POS, website, and third-party platforms (DoorDash, Uber Eats, Grubhub) via their APIs
- **Update handler**: propagate price changes, 86'd items, and seasonal rotations across all platforms simultaneously

Change your pricing source or categorization scheme and the menu pipeline adapts transparently.

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
menu-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/menumanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MenuManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CategorizeWorker.java
│       ├── CreateItemsWorker.java
│       ├── PriceWorker.java
│       ├── PublishWorker.java
│       └── UpdateWorker.java
└── src/test/java/menumanagement/workers/
    ├── CreateItemsWorkerTest.java
    └── UpdateWorkerTest.java

```
