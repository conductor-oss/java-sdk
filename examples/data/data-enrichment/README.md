# Data Enrichment in Java Using Conductor: Geographic, Company, and Credit Lookups with Record Merging

Marketing hands you a spreadsheet of 10,000 leads. Each row has a name, an email, and a zip code. Sales asks: "Which of these are at enterprise companies? What industry? What's their credit risk?" You can't answer any of that from the raw data. So you write a script that calls a geocoding API for each zip, a company-lookup API for each email domain, and a credit bureau API for each record. The geocoding API rate-limits you at row 500. Your script crashes. You restart it and re-enrich all 500 rows you already processed because there's no checkpoint. Then the company-lookup API returns a 503 on row 2,400, and you start over again. Each enrichment source is a different API with different failure modes, and your monolithic script treats them all the same. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You have a dataset of customer or lead records with basic fields. Name, email, zip code; but your sales, marketing, and risk teams need richer data to do their jobs. They need geographic context (which city and timezone is this person in?), company intelligence (how big is their employer? what industry?), and credit signals (what's the credit tier for underwriting?). Each enrichment layer comes from a different source and must be applied in sequence because later lookups may depend on earlier results.

Without orchestration, you'd write a single enrichment method that calls a geocoding API, then a company lookup API, then a credit bureau API, all inline. If the company API rate-limits you, the entire pipeline stalls. If the process crashes after geo enrichment but before credit lookup, you'd re-enrich everything from scratch. Adding a new enrichment source (social media profiles, technographic data) means rewriting tightly coupled code with no visibility into which lookup is slow or failing.

## The Solution

**You just write the geo lookup, company lookup, credit lookup, and merge workers. Conductor handles sequential enrichment passes, automatic retries when third-party APIs are rate-limited, and crash recovery that resumes from the exact enrichment step that failed.**

Each enrichment source is a simple, independent worker. The geo lookup worker resolves zip codes to city, state, and timezone. The company lookup worker maps email domains to company profiles (industry, employee count, revenue range). The credit lookup worker retrieves credit scores and risk tiers. The merger combines all enrichment layers with the original records and summarizes how many fields were added. Conductor executes them in sequence, passes progressively enriched records between steps, retries if an API call is rate-limited or times out, and resumes from the exact enrichment step where it left off if the process crashes.

### What You Write: Workers

Five workers handle multi-source enrichment: loading customer records, resolving geographic data from zip codes, looking up company firmographics from email domains, retrieving credit scores, and merging all layers into a unified output.

| Worker | Task | What It Does |
|---|---|---|
| **LoadRecordsWorker** | `dr_load_records` | Loads incoming records for enrichment. |
| **LookupCompanyWorker** | `dr_lookup_company` | Enriches records with company data based on email domain lookup. |
| **LookupCreditWorker** | `dr_lookup_credit` | Enriches records with fixed credit score data. |
| **LookupGeoWorker** | `dr_lookup_geo` | Enriches records with geographic data based on zip code lookup. |
| **MergeEnrichedWorker** | `dr_merge_enriched` | Merges all enriched data and produces a summary. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
