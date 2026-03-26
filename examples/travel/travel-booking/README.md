# Travel Booking

Travel booking: search, compare, book, confirm, itinerary.

**Input:** `travelerId`, `origin`, `destination`, `departDate` | **Timeout:** 60s

## Pipeline

```
tvb_search
    │
tvb_compare
    │
tvb_book
    │
tvb_confirm
    │
tvb_itinerary
```

## Workers

**BookWorker** (`tvb_book`): Books the selected travel option. Real booking confirmation.

```java
boolean withinBudget = Boolean.TRUE.equals(withinBudgetObj);
```

Reads `withinBudget`. Outputs `bookingRef`, `booked`, `bookedAt`.

**CompareWorker** (`tvb_compare`): Compares search results and selects best option. Real comparison logic.

```java
boolean withinBudget = best != null;
```

Reads `budget`, `searchResults`. Outputs `bestOption`, `withinBudget`, `optionsCompared`.

**ConfirmWorker** (`tvb_confirm`): Confirms booking and sends confirmation.

```java
boolean booked = Boolean.TRUE.equals(bookedObj);
```

Reads `booked`, `bookingRef`. Outputs `confirmed`, `confirmationSent`.

**ItineraryWorker** (`tvb_itinerary`): Builds travel itinerary. Real itinerary construction.

```java
itinerary.add(Map.of("day", departureDate, "activity", "Depart to " + destination));
```

Reads `departureDate`, `destination`, `returnDate`. Outputs `itinerary`, `totalDays`.

**SearchWorker** (`tvb_search`): Searches for available travel options. Real price computation based on destination and dates.

```java
double basePrice = BASE_PRICES.getOrDefault(destination.toLowerCase(), 600.0 + Math.abs(destination.hashCode() % 500));
```

Reads `destination`. Outputs `searchResults`, `resultCount`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
