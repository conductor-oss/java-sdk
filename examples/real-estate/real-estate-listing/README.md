# Real Estate Listing in Java with Conductor : Create, Verify, Enrich, Publish, and Distribute

## The Problem

You need to get a property listed and visible to buyers. The agent enters the address, price, and details; but before the listing goes live, the data must be verified (correct address, valid price range, no duplicate listings), enriched with professional photos, school district info, and walk scores, published to the MLS, and then distributed to consumer-facing portals. If the listing is published before verification, bad data reaches buyers. If distribution fails for one portal, the listing has inconsistent reach.

Without orchestration, listing creation is a manual process across multiple systems. The agent enters data in the MLS portal, separately uploads photos to Zillow, manually posts to Realtor.com, and hopes everything is consistent. A monolithic script that tries to automate this breaks when Zillow's API is down, and nobody knows which portals received the listing and which didn't.

## The Solution

**You just write the listing creation, data verification, photo enrichment, MLS publishing, and portal syndication logic. Conductor handles photo processing retries, MLS publication, and listing audit trails.**

Each listing step is a simple, independent worker. one creates the listing record, one verifies accuracy, one enriches with supplementary data, one publishes to the MLS, one distributes to syndication channels. Conductor takes care of executing them in order, retrying if a portal API is temporarily unavailable, and tracking the listing lifecycle from creation through full distribution.

### What You Write: Workers

Property intake, photo processing, listing composition, and MLS publication workers each handle one step of bringing a property to market.

| Worker | Task | What It Does |
|---|---|---|
| **CreateListingWorker** | `rel_create` | Creates the listing record with address, price, bedrooms, bathrooms, and agent details |
| **VerifyListingWorker** | `rel_verify` | Validates data accuracy. correct address format, reasonable price range, no duplicate MLS entries |
| **EnrichListingWorker** | `rel_enrich` | Adds photos, virtual tour links, school ratings, walk scores, and neighborhood demographics |
| **PublishListingWorker** | `rel_publish` | Publishes the enriched listing to the MLS and assigns a listing ID |
| **DistributeListingWorker** | `rel_distribute` | Syndicates the listing to consumer portals (Zillow, Realtor.com, Redfin, Trulia) |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
rel_create
 │
 ▼
rel_verify
 │
 ▼
rel_enrich
 │
 ▼
rel_publish
 │
 ▼
rel_distribute

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
