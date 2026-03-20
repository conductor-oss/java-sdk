# Real Estate Listing in Java with Conductor :  Create, Verify, Enrich, Publish, and Distribute

A Java Conductor workflow example for publishing property listings .  creating the listing with address and price, verifying data accuracy, enriching with photos and neighborhood data, publishing to the MLS, and distributing to syndication channels (Zillow, Realtor.com, Redfin). Uses [Conductor](https://github.## The Problem

You need to get a property listed and visible to buyers. The agent enters the address, price, and details; but before the listing goes live, the data must be verified (correct address, valid price range, no duplicate listings), enriched with professional photos, school district info, and walk scores, published to the MLS, and then distributed to consumer-facing portals. If the listing is published before verification, bad data reaches buyers. If distribution fails for one portal, the listing has inconsistent reach.

Without orchestration, listing creation is a manual process across multiple systems. The agent enters data in the MLS portal, separately uploads photos to Zillow, manually posts to Realtor.com, and hopes everything is consistent. A monolithic script that tries to automate this breaks when Zillow's API is down, and nobody knows which portals received the listing and which didn't.

## The Solution

**You just write the listing creation, data verification, photo enrichment, MLS publishing, and portal syndication logic. Conductor handles photo processing retries, MLS publication, and listing audit trails.**

Each listing step is a simple, independent worker .  one creates the listing record, one verifies accuracy, one enriches with supplementary data, one publishes to the MLS, one distributes to syndication channels. Conductor takes care of executing them in order, retrying if a portal API is temporarily unavailable, and tracking the listing lifecycle from creation through full distribution. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Property intake, photo processing, listing composition, and MLS publication workers each handle one step of bringing a property to market.

| Worker | Task | What It Does |
|---|---|---|
| **CreateListingWorker** | `rel_create` | Creates the listing record with address, price, bedrooms, bathrooms, and agent details |
| **VerifyListingWorker** | `rel_verify` | Validates data accuracy .  correct address format, reasonable price range, no duplicate MLS entries |
| **EnrichListingWorker** | `rel_enrich` | Adds photos, virtual tour links, school ratings, walk scores, and neighborhood demographics |
| **PublishListingWorker** | `rel_publish` | Publishes the enriched listing to the MLS and assigns a listing ID |
| **DistributeListingWorker** | `rel_distribute` | Syndicates the listing to consumer portals (Zillow, Realtor.com, Redfin, Trulia) |

Workers simulate property transaction steps .  listing, inspection, escrow, closing ,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/real-estate-listing-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/real-estate-listing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rel_real_estate_listing \
  --version 1 \
  --input '{"address": "test-value", "price": 100, "agentId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rel_real_estate_listing -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real listing systems .  your MLS feed for publishing, Zillow and Realtor.com APIs for syndication, a photo hosting service for media enrichment, and the workflow runs identically in production.

- **CreateListingWorker** (`rel_create`): accept listing data from your CRM or agent portal, validate against MLS data standards (RESO/RETS)
- **VerifyListingWorker** (`rel_verify`): query county assessor records to validate address and square footage, check MLS for duplicate listings
- **EnrichListingWorker** (`rel_enrich`): pull photos from Matterport/cloud storage, fetch school ratings from GreatSchools API, get Walk Score data
- **PublishListingWorker** (`rel_publish`): submit to your MLS via RETS/RESO Web API, set listing status to Active
- **DistributeListingWorker** (`rel_distribute`): syndicate via ListHub, push to Zillow Bridge API, post to Realtor.com and Redfin feeds

Switch MLS platforms or photo services and the listing pipeline handles them without restructuring.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
real-estate-listing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/realestatelisting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RealEstateListingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateListingWorker.java
│       ├── DistributeListingWorker.java
│       ├── EnrichListingWorker.java
│       ├── PublishListingWorker.java
│       └── VerifyListingWorker.java
└── src/test/java/realestatelisting/workers/
    ├── CreateListingWorkerTest.java        # 2 tests
    ├── DistributeListingWorkerTest.java        # 2 tests
    ├── EnrichListingWorkerTest.java        # 2 tests
    ├── PublishListingWorkerTest.java        # 2 tests
    └── VerifyListingWorkerTest.java        # 2 tests
```
