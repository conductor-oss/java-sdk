# Kitchen Workflow in Java with Conductor

Manages the kitchen flow for a restaurant order: receiving it from the POS, prepping ingredients, cooking, plating with garnishes, and serving to the table. ## The Problem

You need to manage the kitchen workflow for a restaurant order. from the moment it arrives in the kitchen to when it reaches the customer's table. The order is received from the POS system, ingredients are prepped (chopping, marinating, portioning), the dishes are cooked, plated with garnishes and presentation, and served to the table. Kitchen delays cascade, late prep means late cooking, which means cold plated food sitting under heat lamps.

Without orchestration, you'd manage kitchen flow with verbal communication and paper tickets. manually tracking which orders are at which station, handling ticket pile-ups during rush hours, and coordinating timing so all dishes for a table come out together.

## The Solution

**You just write the order receiving, ingredient prep, cooking, plating, and table service logic. Conductor handles station handoff retries, prep sequencing, and kitchen order tracking.**

Each kitchen step is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (receive, prep, cook, plate, serve), tracking every order through the kitchen with timestamps at each station, and resuming from the last step if the process crashes. ### What You Write: Workers

Order receipt, prep assignment, cooking, plating, and dispatch workers model the kitchen line as a series of handoffs between stations.

| Worker | Task | What It Does |
|---|---|---|
| **CookWorker** | `kit_cook` | Cooks the order, tracking cook time and internal temperature (e.g., 165F) |
| **PlateWorker** | `kit_plate` | Plates the order with garnish and presentation styling |
| **PrepWorker** | `kit_prep` | Preps ingredients (chopping, marinating, portioning) for the order and returns prep time |
| **ReceiveOrderWorker** | `kit_receive_order` | Receives the order in the kitchen, parses items (e.g., Salmon, Risotto, Salad), and assigns the station |
| **ServeWorker** | `kit_serve` | Serves the completed order to the table and records total time from receipt to service |

### The Workflow

```
kit_receive_order
 │
 ▼
kit_prep
 │
 ▼
kit_cook
 │
 ▼
kit_plate
 │
 ▼
kit_serve

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
