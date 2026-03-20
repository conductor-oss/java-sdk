# Data Enrichment in Java Using Conductor: Geographic, Company, and Credit Lookups with Record Merging

Marketing hands you a spreadsheet of 10,000 leads. Each row has a name, an email, and a zip code. Sales asks: "Which of these are at enterprise companies? What industry? What's their credit risk?" You can't answer any of that from the raw data. So you write a script that calls a geocoding API for each zip, a company-lookup API for each email domain, and a credit bureau API for each record. The geocoding API rate-limits you at row 500. Your script crashes. You restart it and re-enrich all 500 rows you already processed because there's no checkpoint. Then the company-lookup API returns a 503 on row 2,400, and you start over again. Each enrichment source is a different API with different failure modes, and your monolithic script treats them all the same. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You have a dataset of customer or lead records with basic fields. Name, email, zip code; but your sales, marketing, and risk teams need richer data to do their jobs. They need geographic context (which city and timezone is this person in?), company intelligence (how big is their employer? what industry?), and credit signals (what's the credit tier for underwriting?). Each enrichment layer comes from a different source and must be applied in sequence because later lookups may depend on earlier results.

Without orchestration, you'd write a single enrichment method that calls a geocoding API, then a company lookup API, then a credit bureau API, all inline. If the company API rate-limits you, the entire pipeline stalls. If the process crashes after geo enrichment but before credit lookup, you'd re-enrich everything from scratch. Adding a new enrichment source (social media profiles, technographic data) means rewriting tightly coupled code with no visibility into which lookup is slow or failing.

## The Solution

**You just write the geo lookup, company lookup, credit lookup, and merge workers. Conductor handles sequential enrichment passes, automatic retries when third-party APIs are rate-limited, and crash recovery that resumes from the exact enrichment step that failed.**

Each enrichment source is a simple, independent worker. The geo lookup worker resolves zip codes to city, state, and timezone. The company lookup worker maps email domains to company profiles (industry, employee count, revenue range). The credit lookup worker retrieves credit scores and risk tiers. The merger combines all enrichment layers with the original records and summarizes how many fields were added. Conductor executes them in sequence, passes progressively enriched records between steps, retries if an API call is rate-limited or times out, and resumes from the exact enrichment step where it left off if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle multi-source enrichment: loading customer records, resolving geographic data from zip codes, looking up company firmographics from email domains, retrieving credit scores, and merging all layers into a unified output.

| Worker | Task | What It Does |
|---|---|---|
| **LoadRecordsWorker** | `dr_load_records` | Loads incoming records for enrichment. |
| **LookupCompanyWorker** | `dr_lookup_company` | Enriches records with company data based on email domain lookup. |
| **LookupCreditWorker** | `dr_lookup_credit` | Enriches records with fixed credit score data. |
| **LookupGeoWorker** | `dr_lookup_geo` | Enriches records with geographic data based on zip code lookup. |
| **MergeEnrichedWorker** | `dr_merge_enriched` | Merges all enriched data and produces a summary. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dr_load_records
    │
    ▼
dr_lookup_geo
    │
    ▼
dr_lookup_company
    │
    ▼
dr_lookup_credit
    │
    ▼
dr_merge_enriched
```

## Example Output

```
=== Data Enrichment Workflow Demo ===

Step 1: Registering task definitions...
  Registered: dr_load_records, dr_lookup_geo, dr_lookup_company, dr_lookup_credit, dr_merge_enriched

Step 2: Registering workflow 'data_enrichment'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 47bcf0b4-a603-a917-7ba2-f32acb0eaf34

  [load] Loaded 3 records for enrichment
  [geo] Enriched 3 records with geo data
  [company] Enriched 3 records with company data
  [credit] Enriched 3 records with credit scores
  [merge] Enriched 


  Status: COMPLETED
  Output: {originalCount=3, enrichedCount=3, fieldsAdded=3, summary=Enriched }

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/data-enrichment-1.0.0.jar
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
java -jar target/data-enrichment-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_enrichment \
  --version 1 \
  --input '{"records": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_enrichment -s COMPLETED -c 5
```

## How to Extend

Connect the geo worker to Google Maps, the company worker to Clearbit or ZoomInfo, and the credit worker to Experian, the multi-source enrichment workflow runs unchanged.

- **LoadRecordsWorker** → read records from a CRM (Salesforce, HubSpot), a database, or a CSV upload
- **LookupGeoWorker** → call a real geocoding API (Google Maps, Mapbox, or a zip code database) to resolve geographic details from addresses or zip codes
- **LookupCompanyWorker** → integrate Clearbit, ZoomInfo, or LinkedIn API to look up company firmographic data from email domains
- **LookupCreditWorker** → call Experian, Equifax, or TransUnion APIs for real credit scores and risk assessments
- **MergeEnrichedWorker** → write the enriched records to your data warehouse, CRM, or downstream analytics pipeline

Pointing any lookup worker at a different data provider requires no pipeline changes, as long as enrichment fields like city, industry, or credit tier are returned in the expected format.

**Add new enrichment layers** by inserting tasks in `workflow.json`, for example, social media profile lookups, technographic enrichment (what tech stack does the company use), intent data from Bombora, or phone number validation via Twilio Lookup.

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
data-enrichment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataenrichment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataEnrichmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── LoadRecordsWorker.java
│       ├── LookupCompanyWorker.java
│       ├── LookupCreditWorker.java
│       ├── LookupGeoWorker.java
│       └── MergeEnrichedWorker.java
└── src/test/java/dataenrichment/workers/
    ├── LoadRecordsWorkerTest.java        # 8 tests
    ├── LookupCompanyWorkerTest.java        # 9 tests
    ├── LookupCreditWorkerTest.java        # 9 tests
    ├── LookupGeoWorkerTest.java        # 9 tests
    └── MergeEnrichedWorkerTest.java        # 9 tests
```
