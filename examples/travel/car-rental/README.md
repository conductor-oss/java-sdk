# Car Rental in Java with Conductor

Car rental: search, select, book, pickup, return. ## The Problem

You need to manage a car rental for a business traveler. Searching available vehicles at the pickup location, selecting the right vehicle class (compact, midsize, SUV) based on traveler needs and company policy, booking the reservation, processing the vehicle pickup with documentation, and handling the vehicle return with final charges. Each step depends on the previous one's output.

If the booking succeeds but the selected vehicle class is unavailable at pickup, you need the reservation details to arrange an upgrade or alternate vehicle. If the return step fails to record mileage and fuel level, the final charges are wrong and the company disputes the invoice. Without orchestration, you'd build a monolithic rental handler that mixes fleet inventory queries, policy checks, reservation API calls, and return processing. Making it impossible to swap rental providers, test vehicle selection logic independently, or track which policy rules drove which vehicle class selection.

## The Solution

**You just write the vehicle search, class selection, booking, pickup inspection, and return processing logic. Conductor handles availability retries, reservation sequencing, and rental audit trails.**

SearchWorker queries available rental vehicles at the pickup location and dates. SelectWorker picks the best vehicle class based on the traveler's needs and company policy (midsize default, SUV for group travel). BookWorker reserves the selected vehicle and returns a reservation number. PickupWorker processes the vehicle pickup. Recording the odometer reading, fuel level, and damage inspection. ReturnWorker handles the vehicle return, calculates final charges (mileage, fuel, insurance), and closes the rental. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Availability lookup, rate comparison, reservation, and pickup confirmation workers each manage one stage of the car rental booking process.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `crl_book` | Books the input and computes reservation id, confirmation code |
| **PickupWorker** | `crl_pickup` | Vehicle picked up. reservation |
| **ReturnWorker** | `crl_return` | Processes the vehicle return. Records the reservation as returned, calculates total cost, and captures ending mileage |
| **SearchWorker** | `crl_search` | Searching rentals at |
| **SelectWorker** | `crl_select` | Selected midsize vehicle |

### The Workflow

```
crl_search
 │
 ▼
crl_select
 │
 ▼
crl_book
 │
 ▼
crl_pickup
 │
 ▼
crl_return

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
