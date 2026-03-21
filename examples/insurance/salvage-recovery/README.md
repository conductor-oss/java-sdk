# Salvage Recovery in Java with Conductor : Assess Damage, Salvage, Auction, Settle, Close

## Total Loss Vehicles Need Salvage, Not Repair

A 2020 sedan with $18K in damage and a pre-loss value of $22K. repair cost exceeds the total loss threshold (typically 70-80% of value). The vehicle is a total loss. The salvage recovery process assesses the damage and confirms total loss, arranges salvage (tow the vehicle to a salvage yard), auctions the salvage (sell the wreck for parts value), settles with the policyholder (pay the actual cash value minus deductible, minus salvage proceeds if the owner retains the vehicle), and closes the claim.

Salvage recovery directly impacts the insurer's bottom line. the auction proceeds offset the claim payment. A $22K claim with $4K in salvage recovery nets to $18K. Efficient salvage handling (fast towing, competitive auction, quick settlement) reduces loss adjustment expenses and improves the combined ratio.

## The Solution

**You just write the damage assessment, salvage valuation, auction listing, settlement, and claim closure logic. Conductor handles auction retries, valuation sequencing, and salvage recovery audit trails.**

`AssessDamageWorker` evaluates the vehicle damage against the pre-loss value to confirm total loss designation and determine the actual cash value. `SalvageWorker` arranges towing to a salvage facility and manages storage until auction. `AuctionWorker` lists the salvage vehicle with auction partners, manages bids, and tracks sale proceeds. `SettleWorker` calculates and issues the settlement payment. actual cash value minus deductible, with any salvage retention adjustments. `CloseWorker` closes the claim file with final financial reconciliation. Conductor tracks the full salvage lifecycle for loss recovery analytics.

### What You Write: Workers

Damage assessment, salvage valuation, auction coordination, and recovery accounting workers each manage one phase of recovering value from insured losses.

| Worker | Task | What It Does |
|---|---|---|
| **AssessDamageWorker** | `slv_assess_damage` | Assesses the vehicle damage. determines total loss status and calculates the salvage value ($4,200) based on the vehicle condition, year, make, and model |
| **SalvageWorker** | `slv_salvage` | Processes the salvage. obtains the salvage title, arranges towing to the salvage yard, and sets the reserve price based on the assessed salvage value |
| **AuctionWorker** | `slv_auction` | Auctions the salvaged vehicle. lists at the reserve price and records the sale proceeds ($5,100) from the winning bid |
| **SettleWorker** | `slv_settle` | Settles the financial recovery. calculates the recovery amount and net recovery from the auction proceeds against the original claim payout |
| **CloseWorker** | `slv_close` | Closes the salvage claim. records the final recovery amount, marks the claim as closed, and generates the closing statement for audit |

### The Workflow

```
slv_assess_damage
 │
 ▼
slv_salvage
 │
 ▼
slv_auction
 │
 ▼
slv_settle
 │
 ▼
slv_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
