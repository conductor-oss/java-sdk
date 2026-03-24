# Menu Management in Java with Conductor

Manages a restaurant menu lifecycle: creating items with descriptions, setting prices, categorizing into sections, publishing to ordering platforms, and syncing updates.

## The Problem

You need to manage a restaurant's menu lifecycle. creating menu items with descriptions and photos, setting prices based on food costs and margins, organizing items into categories (appetizers, entrees, desserts), publishing the menu to ordering platforms, and updating items as ingredients or prices change. Publishing a menu with incorrect prices erodes margins; outdated items lead to orders the kitchen cannot fulfill.

Without orchestration, you'd manage menus through a combination of spreadsheets, POS admin panels, and third-party delivery platform dashboards. manually keeping prices synchronized across platforms, updating photos and descriptions in multiple places, and hoping no platform shows a discontinued item.

## The Solution

**You just write the item creation, pricing, categorization, platform publishing, and update syncing logic. Conductor handles pricing retries, publication sequencing, and menu change audit trails.**

Each menu concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (create items, price, categorize, publish, update), retrying if a platform API is unavailable, tracking every menu change, and resuming from the last step if the process crashes.

### What You Write: Workers

Item creation, pricing, categorization, and publication workers handle menu updates through discrete, auditable operations.

| Worker | Task | What It Does |
|---|---|---|
| **CategorizeWorker** | `mnu_categorize` | Organizes menu items into categories (entrees, desserts) and returns the categorized menu with item counts |
| **CreateItemsWorker** | `mnu_create_items` | Creates menu items with names and descriptions (e.g., Grilled Salmon, Truffle Pasta, Tiramisu) for the restaurant |
| **PriceWorker** | `mnu_price` | Sets prices for each menu item based on food cost and margin targets |
| **PublishWorker** | `mnu_publish` | Publishes the menu to ordering platforms and returns the menu ID, publish status, and date |
| **UpdateWorker** | `mnu_update` | Confirms the menu is live and synced across all platforms, returning the final menu status and item count |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
