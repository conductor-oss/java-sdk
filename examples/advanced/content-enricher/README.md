# Content Enricher

A product listing arrives with only a SKU and a price. Before it can appear in the catalog, it needs enrichment from multiple sources: product name from the master database, category from the taxonomy service, and stock level from the inventory API. The enricher fetches missing fields and merges them into the original message.

## Pipeline

```
[enr_receive_message]
     |
     v
[enr_lookup_data]
     |
     v
[enr_enrich]
     |
     v
[enr_forward]
```

**Workflow inputs:** `message`, `enrichmentSources`

## Workers

**EnrichWorker** (task: `enr_enrich`)

Enriches the original message with lookup data. Merges all fields from the original message and the lookup data, then adds an ISO-8601 enrichedAt timestamp using the real system clock.

- Captures `instant.now()` timestamps
- Reads `originalMessage`, `lookupData`. Writes `enrichedMessage`

**ForwardWorker** (task: `enr_forward`)

Forwards the enriched message by computing a content hash (fingerprint) of the enriched message and determining a routing destination based on the customerId hash.

- Truncates strings to first 12 character(s), computes sha-256 hashes, uses `math.abs()`, formats output strings
- Reads `enrichedMessage`. Writes `forwarded`, `destination`, `contentHash`

**LookupDataWorker** (task: `enr_lookup_data`)

Fetches URL metadata (title, description, word count, Open Graph tags) using java.net.http.HttpClient. Falls back to defaults when no URL is provided.

- Trims whitespace, applies compiled regex
- Reads `customerId`, `enrichmentSources`. Writes `lookupData`

**ReceiveMessageWorker** (task: `enr_receive_message`)

Receives an incoming message and extracts the customer ID.

- Reads `message`. Writes `message`, `customerId`

---

**35 tests** | Workflow: `enr_content_enricher` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
