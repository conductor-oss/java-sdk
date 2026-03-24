# Auction Workflow

Auction workflow: open bidding, collect bids, close, determine winner, settle

**Input:** `auctionId`, `itemName`, `startingPrice` | **Timeout:** 60s

## Pipeline

```
auc_open_bidding
    │
auc_collect_bids
    │
auc_close_auction
    │
auc_determine_winner
    │
auc_settle
```

## Workers

**CloseAuctionWorker** (`auc_close_auction`)

**CollectBidsWorker** (`auc_collect_bids`)

```java
Map.of("bidderId", "BID-001", "amount", startPrice + 10),
```

**DetermineWinnerWorker** (`auc_determine_winner`)

```java
if (amount > winningBid) { winningBid = amount; winnerId = (String) bid.get("bidderId"); }
```

**OpenBiddingWorker** (`auc_open_bidding`)

**SettleAuctionWorker** (`auc_settle`)

## Tests

**6 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
