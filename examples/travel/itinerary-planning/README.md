# Itinerary Planning in Java with Conductor

Itinerary planning: preferences, search, optimize, book, finalize.

## The Problem

You need to plan a complete travel itinerary for an employee. loading their travel preferences (seat, airline, hotel chain, meal requirements), searching for flights and hotels that match, optimizing the combination for cost and convenience (minimizing layovers, grouping nearby hotels), booking the selected options, and finalizing the itinerary with all confirmation details sent to the traveler. Each step builds on the previous one's output.

If the optimization step selects a cheaper flight but the booking fails because the fare expired, you need to re-optimize without losing the hotel selection. If booking succeeds but finalization fails, the traveler has reservations but no consolidated itinerary document. Without orchestration, you'd build a monolithic planner that mixes preference lookups, GDS queries, optimization algorithms, and booking API calls. making it impossible to swap search providers, test optimization logic independently, or track which preferences drove which booking decisions.

## The Solution

**You just write the preference loading, availability search, itinerary optimization, booking, and finalization logic. Conductor handles research retries, scheduling coordination, and itinerary version tracking.**

PreferencesWorker loads the traveler's saved preferences (preferred airlines, seat type, hotel chains, dietary needs). SearchWorker queries flight and hotel availability for the destination and dates, filtered by those preferences. OptimizeWorker ranks the options by cost and convenience. minimizing total price, layover time, and distance from meeting venues. BookWorker reserves the selected flights and hotels. FinalizeWorker assembles the complete itinerary with all confirmation numbers, check-in times, and directions, then sends it to the traveler. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Destination research, activity selection, scheduling, and itinerary assembly workers each own one aspect of building a coherent travel plan.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `itp_book` | Flight and hotel booked |
| **FinalizeWorker** | `itp_finalize` | Finalizes the itinerary and sends the confirmed travel plan to the traveler |
| **OptimizeWorker** | `itp_optimize` | Optimized for cost and convenience |
| **PreferencesWorker** | `itp_preferences` | Loads the traveler's preferences (budget, travel style, dietary needs) |
| **SearchWorker** | `itp_search` | Searches for flights, hotels, and activities matching the trip criteria |

### The Workflow

```
itp_preferences
 │
 ▼
itp_search
 │
 ▼
itp_optimize
 │
 ▼
itp_book
 │
 ▼
itp_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
