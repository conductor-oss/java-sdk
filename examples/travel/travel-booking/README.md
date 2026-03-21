# Travel Booking Workflow in Java with Conductor

A traveler books a flight from SFO to JFK, a hotel in Manhattan, and then the car rental fails. the rental company's API returns a 503 at 2 AM. Now they have a confirmed flight landing at JFK, a hotel room waiting in Midtown, and no way to get from the airport. The flight is non-refundable. The hotel has a 24-hour cancellation policy that expires in 6 hours. Nobody is awake to notice the partial failure, and the traveler finds out when they land and check their itinerary. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a multi-step booking pipeline, you write the booking logic, Conductor handles retries, failure routing, durability, and observability.

## The Multi-Step Booking Problem

A user wants to fly from SFO to JFK on April 15th. The system needs to search three airlines for available flights, compare them on price and duration, reserve the cheapest option, confirm the booking and issue an e-ticket, and email the complete itinerary, all in the right order. If the booking succeeds but confirmation fails, you have a reserved seat with no e-ticket. If the search returns stale results and the selected flight sells out before booking, you need to restart from comparison without losing the traveler's preferences.

Without orchestration, you'd build a monolithic booking script that mixes GDS queries, fare comparison logic, reservation API calls, and email sending, making it impossible to swap airlines, test fare comparison independently, or audit which search results led to which booking.

## The Solution

**You just write the flight search, fare comparison, booking, confirmation, and itinerary delivery logic. Conductor handles booking retries with idempotency, search-to-confirmation sequencing, and full trip audit trails.**

Each worker handles one travel operation. Conductor manages the booking pipeline, approval gates, policy enforcement, and itinerary tracking.

### What You Write: Workers

Five workers divide the booking flow. Search, compare, book, confirm, and itinerary delivery, so flight search logic stays separate from payment and ticketing.

| Worker | Task | What It Does |
|---|---|---|
| `SearchWorker` | `tvb_search` | Queries available flights for the given origin/destination/date, returning 3 options (United $450, Delta $420, AA $480) |
| `CompareWorker` | `tvb_compare` | Evaluates search results and selects Delta flight DL-1234 at $420 as the best value |
| `BookWorker` | `tvb_book` | Reserves the selected flight for the traveler and returns booking ID `BK-travel-booking` with confirmation code |
| `ConfirmWorker` | `tvb_confirm` | Finalizes the reservation, issuing e-ticket `ET-travel-booking-2024` |
| `ItineraryWorker` | `tvb_itinerary` | Assembles and sends the complete itinerary (flight, booking ref, e-ticket) to the traveler |

### The Workflow

```
tvb_search
 |
 v
tvb_compare
 |
 v
tvb_book
 |
 v
tvb_confirm
 |
 v
tvb_itinerary

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
