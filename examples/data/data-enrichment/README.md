# Data Enrichment

An e-commerce platform stores product records with bare SKUs, prices, and titles. Marketing needs enriched listings that include category taxonomies, computed price tiers, normalized descriptions, and quality scores. Each enrichment step depends on the previous one's output, and a failure in scoring should not wipe out the taxonomy work already done.

## Pipeline

```
[dr_load_records]
     |
     v
[dr_lookup_geo]
     |
     v
[dr_lookup_company]
     |
     v
[dr_lookup_credit]
     |
     v
[dr_merge_enriched]
```

**Workflow inputs:** `records`

## Workers

**LoadRecordsWorker** (task: `dr_load_records`)

Loads incoming records for enrichment.

- Reads `records`. Writes `records`, `count`

**LookupCompanyWorker** (task: `dr_lookup_company`)

Enriches records with company data based on email domain lookup.

- `DEFAULT_COMPANY` (Map)
- Reads `records`. Writes `enriched`

**LookupCreditWorker** (task: `dr_lookup_credit`)

Enriches records with computed credit scoring based on record data. The credit score is computed from available fields: - Base score: 650 - Has email: +30 - Has company data: +40 - Has geo data (known location): +30 - Name length > 3 chars: +20 - Has phone: +30 Tier is computed from score: excellent (750+), good (700+), fair (650+), poor (<650)

- Reads `records`. Writes `enriched`

**LookupGeoWorker** (task: `dr_lookup_geo`)

Enriches records with geographic data. Performs real lookups: - If record has "ip" field: does real reverse DNS lookup to get hostname - If record has "hostname" field: does real forward DNS lookup to get IP - If record has "zip" field: uses a built-in US zip code database All lookups are real -- DNS lookups use java.net.InetAddress.

- `DEFAULT_GEO` (Map)
- Reads `records`. Writes `enriched`

**MergeEnrichedWorker** (task: `dr_merge_enriched`)

Merges all enriched data and produces a summary by counting how many enrichment fields were actually added to records.

- `ENRICHMENT_FIELDS` = Set.of("geo", "company", "credit", "dns")
- Formats output strings
- Reads `records`, `originalCount`. Writes `enrichedCount`, `fieldsAdded`, `summary`, `records`

---

**52 tests** | Workflow: `data_enrichment` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
