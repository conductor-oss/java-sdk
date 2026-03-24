# Service Orchestration in Java with Conductor

Orchestrate auth, catalog, cart, and checkout microservices.

## The Problem

A typical e-commerce purchase flow spans four microservices: authenticate the user, look up the product in the catalog, add it to the shopping cart, and process checkout. Each step depends on the output of the previous one, the catalog lookup needs an auth token, the cart needs the product details, and checkout needs the cart total.

Without orchestration, the frontend or a BFF layer chains four HTTP calls with manual error handling. If the catalog service is slow, the entire request hangs with no timeout isolation. There is no single place to see the full purchase flow or retry a failed step.

## The Solution

**You just write the authentication, catalog-lookup, cart, and checkout workers. Conductor handles auth-to-checkout sequencing, per-step timeout isolation, and complete purchase flow traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers drive the purchase flow: AuthenticateWorker validates user credentials, CatalogLookupWorker retrieves product details, AddToCartWorker manages the shopping cart, and CheckoutWorker processes the final payment.

| Worker | Task | What It Does |
|---|---|---|
| **AddToCartWorker** | `so_add_to_cart` | Adds items to a shopping cart. |
| **AuthenticateWorker** | `so_authenticate` | Authenticates a user and returns a JWT token. |
| **CatalogLookupWorker** | `so_catalog_lookup` | Looks up a product in the catalog. |
| **CheckoutWorker** | `so_checkout` | Processes checkout for a cart. |

the workflow coordination stays the same.

### The Workflow

```
so_authenticate
 │
 ▼
so_catalog_lookup
 │
 ▼
so_add_to_cart
 │
 ▼
so_checkout

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
