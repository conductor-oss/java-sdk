# Xml Parsing

A supply chain system receives purchase orders as XML documents from EDI partners. Each XML needs schema validation against an XSD, parsing into a structured Java object, field-level transformation (date formats, unit conversions), and output as a normalized JSON record for the order management system.

## Pipeline

```
[xp_receive_xml]
     |
     v
[xp_parse_tags]
     |
     v
[xp_extract_fields]
     |
     v
[xp_convert_to_json]
     |
     v
[xp_emit_records]
```

**Workflow inputs:** `xmlContent`, `rootElement`

## Workers

**ConvertToJsonWorker** (task: `xp_convert_to_json`)

Converts extracted data to JSON records by adding source and parsedAt metadata.

- Reads `extractedData`. Writes `jsonRecords`, `count`

**EmitRecordsWorker** (task: `xp_emit_records`)

Emits the final parsed records with a completion status.

- Sets `status` = `"XML_TO_JSON_COMPLETE"`
- Reads `recordCount`. Writes `status`, `recordCount`

**ExtractFieldsWorker** (task: `xp_extract_fields`)

Extracts id, name, price (as double), and category from each parsed element.

- Parses strings to `double`
- Reads `elements`. Writes `records`, `fieldCount`

**ParseTagsWorker** (task: `xp_parse_tags`)

Parses XML tags and returns deterministic.parsed elements.

- Reads `xml`, `rootElement`. Writes `elements`, `elementCount`

**ReceiveXmlWorker** (task: `xp_receive_xml`)

Receives raw XML content and passes it along with metadata.

- Reads `xmlContent`, `rootElement`. Writes `xml`, `rootElement`, `xmlSize`

---

**44 tests** | Workflow: `xml_parsing_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
