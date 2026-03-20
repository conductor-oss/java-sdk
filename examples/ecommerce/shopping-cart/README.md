# Shopping Cart in Java Using Conductor :  Add Items, Calculate Total, Apply Discounts, Reserve Inventory

A Java Conductor workflow example for shopping cart processing .  adding items to a cart, calculating the subtotal, applying coupon-code discounts, and reserving inventory so items are not oversold before checkout. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Shopping Carts Are More Complex Than They Look

A customer adds 3 items to their cart with a coupon code. The system must validate each item (in stock, valid price, quantity limits), calculate the correct total (unit prices, quantity discounts, bundle pricing), apply the coupon (validate code, check eligibility, calculate discount), and reserve inventory (so the items aren't sold out before checkout).

The order matters: items must be added before totals can be calculated, totals must be calculated before discounts are applied (a percentage discount depends on the subtotal), and inventory should be reserved after discounts are confirmed (so abandoned carts with invalid coupons don't hold inventory unnecessarily). If the inventory reservation fails for one item (sold out since adding to cart), the cart total needs recalculation.

## The Solution

**You just write the item validation, total calculation, discount application, and inventory reservation logic. Conductor handles pricing retries, cart state durability, and complete cart lifecycle tracking.**

`AddItemsWorker` validates and adds items to the cart .  checking availability, enforcing quantity limits, and confirming current pricing. `CalculateTotalWorker` computes the subtotal from item prices and quantities, applying any quantity-based or bundle pricing rules. `ApplyDiscountsWorker` processes coupon codes and promotional discounts ,  validating eligibility, calculating the discount amount, and applying it to the subtotal. `ReserveInventoryWorker` places soft holds on the cart items to prevent overselling during the checkout window. Conductor chains these four steps and records the cart state at each stage for conversion analytics.

### What You Write: Workers

Cart creation, item management, pricing, and checkout-readiness workers operate on cart state independently, keeping pricing logic separate from inventory checks.

| Worker | Task | What It Does |
|---|---|---|
| **AddItemsWorker** | `cart_add_items` | Validates items and adds them to the cart, assigning a cart ID and line item IDs |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cart_add_items
    │
    ▼
cart_calculate_total
    │
    ▼
cart_apply_discounts
    │
    ▼
cart_reserve_inventory
```

## Example Output

```
=== Shopping Cart Demo ===

Step 1: Registering task definitions...
  Registered: cart_add_items, cart_calculate_total, cart_apply_discounts, cart_reserve_inventory

Step 2: Registering workflow 'shopping_cart'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [add] Cart
  [discount] Cart
  [total] Cart
  [reserve] Cart

  Status: COMPLETED
  Output: {lineId=..., addedAt=..., cartId=..., cartItems=...}

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
java -jar target/shopping-cart-1.0.0.jar
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
java -jar target/shopping-cart-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow shopping_cart \
  --version 1 \
  --input '{"userId": "user-42", "user-42": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w shopping_cart -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real commerce stack .  your inventory API for item validation, your pricing engine for totals, your coupon database for discounts, and the workflow runs identically in production.

- **CalculateTotalWorker** (`cart_calculate_total`): implement complex pricing: tiered quantity discounts, bundle pricing, member pricing, and multi-currency support with real-time exchange rates
- **ApplyDiscountsWorker** (`cart_apply_discounts`): integrate with your coupon engine for code validation, support stackable promotions, and calculate the optimal discount combination when multiple promotions apply
- **ReserveInventoryWorker** (`cart_reserve_inventory`): use Redis with TTL-based reservations that auto-expire if the cart is abandoned, preventing permanent inventory locks

Replace your pricing engine or inventory checker and the cart workflow keeps running with the same structure.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
shopping-cart/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/shoppingcart/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ShoppingCartExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── AddItemsWorker.java
```
