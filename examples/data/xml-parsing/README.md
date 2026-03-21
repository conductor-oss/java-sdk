# XML Parsing in Java Using Conductor : XML Reception, Tag Parsing, Field Extraction, JSON Conversion, and Record Emission

## The Problem

Your partners send product catalog updates as XML files. Each file contains nested elements under a configurable root tag, and each element has fields like `id`, `name`, `price`, and `category` buried inside XML tags. Your downstream systems: the product database, the search index, the pricing engine, all consume JSON. You need to receive the XML, parse it into its constituent elements under the root tag, extract typed fields from each element (treating `price` as a double, not a string), convert the extracted data to JSON records with metadata (source, parsedAt timestamp), and emit the records for downstream consumers. Each step depends on the previous one: you can't extract fields from unparsed XML, and you can't convert to JSON without typed field values.

Without orchestration, you'd write a single JAXB or DOM parser method that reads XML, extracts fields, builds JSON, and outputs records in one pass. If the field extraction logic breaks on an unexpected XML structure (say, a `price` element with currency attributes), the entire parse fails with no visibility into which element caused the issue. There's no record of how many elements were parsed vs, how many fields were extracted vs, how many records were emitted. Re-running the pipeline after fixing an extraction bug means re-parsing the entire XML file from scratch.

## The Solution

**You just write the XML reception, tag parsing, field extraction, JSON conversion, and record emission workers. Conductor handles the multi-stage XML pipeline, retries when parsing fails on unexpected structures, and element-by-element count tracking across every stage.**

Each stage of the XML pipeline is a simple, independent worker. The receiver accepts the raw XML content and the root element name, validating that the XML is well-formed. The tag parser walks the XML structure and extracts elements under the specified root tag, producing a list of parsed elements. The field extractor pulls typed values from each element: strings for `id`, `name`, and `category`, doubles for `price`, producing clean records. The JSON converter transforms the extracted records into JSON format, adding metadata like source identifier and `parsedAt` timestamp to each record. The emitter produces the final record set with a completion status and record count. Conductor executes them in strict sequence, passes the evolving data representation between stages, retries if any step fails, and tracks element counts and record counts at every stage. ### What You Write: Workers

Five workers handle XML-to-JSON transformation: receiving raw XML content, parsing tags into structured elements under the root, extracting typed fields (strings, doubles) from each element, converting to JSON records with metadata, and emitting the final dataset.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertToJsonWorker** | `xp_convert_to_json` | Converts extracted data to JSON records by adding source and parsedAt metadata. |
| **EmitRecordsWorker** | `xp_emit_records` | Emits the final parsed records with a completion status. |
| **ExtractFieldsWorker** | `xp_extract_fields` | Extracts id, name, price (as double), and category from each parsed element. |
| **ParseTagsWorker** | `xp_parse_tags` | Parses XML tags and returns demo parsed elements. |
| **ReceiveXmlWorker** | `xp_receive_xml` | Receives raw XML content and passes it along with metadata. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
xp_receive_xml
 │
 ▼
xp_parse_tags
 │
 ▼
xp_extract_fields
 │
 ▼
xp_convert_to_json
 │
 ▼
xp_emit_records

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
