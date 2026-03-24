# Auction Workflow in Java Using Conductor : Open Bidding, Collect Bids, Close, Determine Winner, Settle

Auction workflow: open bidding, collect bids, close, determine winner, settle.

## Auctions Have Strict Lifecycle Rules

An auction for a vintage watch starts at $500. Bids arrive over the next 24 hours. The auction must close at exactly the scheduled time. not a second early or late. The highest valid bid wins, but only if it meets the reserve price. The winner's payment must be processed, the seller must be notified with the final price, and the item must be marked as sold.

Each stage has specific timing and validation requirements. Bidding can only happen during the open period. Each bid must be validated (is the bidder registered? Does the bid exceed the current highest?). The closing must be deterministic. Winner determination must handle edge cases (tied bids, reserve not met). Settlement must charge the winner and release the item to escrow. If payment fails, the next highest bidder should be contacted.

## The Solution

**You just write the bidding, auction closing, winner determination, and settlement logic. Conductor handles bid timing, settlement retries, and complete auction audit trails.**

`OpenBiddingWorker` initializes the auction with item details, starting price, reserve price, and duration. `CollectBidsWorker` accepts and validates bids during the bidding period. checking bidder eligibility, bid amount validity, and incrementing the current price. `CloseAuctionWorker` closes bidding at the scheduled time and locks the bid list. `DetermineWinnerWorker` identifies the highest valid bid, checks against the reserve price, and declares the winner. `SettleWorker` processes payment from the winner, notifies all parties, and updates the item status. Conductor sequences these stages and records every bid for auction transparency.

### What You Write: Workers

Bidding, closing, winner determination, and settlement workers operate on auction state independently, letting you modify bidding rules without touching settlement logic.

| Worker | Task | What It Does |
|---|---|---|
| **CloseAuctionWorker** | `auc_close_auction` | Performs the close auction operation |
| **CollectBidsWorker** | `auc_collect_bids` | Performs the collect bids operation |
| **DetermineWinnerWorker** | `auc_determine_winner` | Handles determine winner |
| **OpenBiddingWorker** | `auc_open_bidding` | Performs the open bidding operation |
| **SettleAuctionWorker** | `auc_settle` | Performs the settle auction operation |

### The Workflow

```
auc_open_bidding
 │
 ▼
auc_collect_bids
 │
 ▼
auc_close_auction
 │
 ▼
auc_determine_winner
 │
 ▼
auc_settle

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
