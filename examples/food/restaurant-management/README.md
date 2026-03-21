# Restaurant Management in Java with Conductor

Manages the full restaurant guest experience: locating a reservation, seating the party, taking a food order, preparing dishes in the kitchen, and generating the bill. ## The Problem

You need to manage the full restaurant guest experience from reservation to checkout. When a guest arrives, their reservation is located, they are seated at an appropriate table, their food order is taken and sent to the kitchen, the kitchen prepares the dishes, and the guest checks out with the bill. A breakdown at any step. lost reservation, wrong table, kitchen miscommunication, billing error, degrades the dining experience.

Without orchestration, you'd coordinate between the host stand, waitstaff, kitchen, and cashier manually. tracking reservations on paper, yelling orders to the kitchen, managing table turns in the host's head, and hoping the POS system correctly captures every item ordered.

## The Solution

**You just write the reservation lookup, seating, order taking, kitchen preparation, and billing logic. Conductor handles scheduling retries, inventory coordination, and operational audit trails.**

Each restaurant operation is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the guest flow (reservations, seating, order, kitchen, checkout), tracking every guest's experience through the restaurant, and resuming from the last step if the process crashes. ### What You Write: Workers

Staff scheduling, inventory ordering, revenue tracking, and compliance workers each address one operational concern of running a restaurant.

| Worker | Task | What It Does |
|---|---|---|
| **CheckoutWorker** | `rst_checkout` | Generates the bill for the table with subtotal, tax, and total |
| **KitchenWorker** | `rst_kitchen` | Prepares the ordered items in the kitchen and returns cook time |
| **OrderWorker** | `rst_order` | Takes the food order at the table and returns an order ID, items (e.g., Steak, Pasta, Wine), and total |
| **ReservationsWorker** | `rst_reservations` | Looks up the guest's reservation by name and party size and returns the confirmed booking |
| **SeatingWorker** | `rst_seating` | Assigns a table and section (e.g., patio) to the reservation and marks the party as seated |

### The Workflow

```
rst_reservations
 │
 ▼
rst_seating
 │
 ▼
rst_order
 │
 ▼
rst_kitchen
 │
 ▼
rst_checkout

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
