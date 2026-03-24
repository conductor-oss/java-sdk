# Event Transformation

An integration layer receives events in a partner's schema that does not match the internal data model. Field names differ, date formats are inconsistent, nested structures need flattening, and some fields require computed values. Each event needs schema transformation before internal consumers can process it.

## Pipeline

```
[et_parse_event]
     |
     v
[et_enrich_event]
     |
     v
[et_map_schema]
     |
     v
[et_output_event]
```

**Workflow inputs:** `rawEvent`, `sourceFormat`, `targetFormat`

## Workers

**EnrichEventWorker** (task: `et_enrich_event`)

Enriches a parsed event with additional context: department, role, and location for the actor; project and environment for the resource; and an enrichment block with geoLocation, riskScore, complianceFlags, and enrichedAt.

- Reads `parsedEvent`, `eventType`. Writes `enrichedEvent`, `fieldsAdded`

**MapSchemaWorker** (task: `et_map_schema`)

Maps an enriched event to CloudEvents format (specversion 1.0). Produces: specversion, type, source, id, time, datacontenttype, subject, and data.

- Sets `type` = `"com.example."`
- Reads `enrichedEvent`, `targetFormat`. Writes `mappedEvent`

**OutputEventWorker** (task: `et_output_event`)

Delivers the mapped event to its target destination and returns delivery metadata: outputEventId, delivered flag, outputSizeBytes, and deliveredAt timestamp.

- Reads `mappedEvent`, `targetFormat`. Writes `outputEventId`, `delivered`, `outputSizeBytes`, `deliveredAt`

**ParseEventWorker** (task: `et_parse_event`)

Parses a raw event from a legacy format into a normalized structure. Extracts and renames fields: event_id->id, event_type->type, ts->timestamp, user_id/user_name->actor, resource_type/resource_id->resource, action, metadata->details.

- Reads `rawEvent`, `sourceFormat`. Writes `parsedEvent`, `eventType`, `fieldCount`

---

**38 tests** | Workflow: `event_transformation_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
