# XML Parsing in Java Using Conductor :  XML Reception, Tag Parsing, Field Extraction, JSON Conversion, and Record Emission

A Java Conductor workflow example for XML-to-JSON transformation. receiving raw XML content with a configurable root element, parsing the XML tags into structured elements, extracting typed fields from each element (id, name, price as double, category), converting the extracted data to JSON records with source metadata and timestamps, and emitting the final records for downstream consumption. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Your partners send product catalog updates as XML files. Each file contains nested elements under a configurable root tag, and each element has fields like `id`, `name`, `price`, and `category` buried inside XML tags. Your downstream systems: the product database, the search index, the pricing engine, all consume JSON. You need to receive the XML, parse it into its constituent elements under the root tag, extract typed fields from each element (treating `price` as a double, not a string), convert the extracted data to JSON records with metadata (source, parsedAt timestamp), and emit the records for downstream consumers. Each step depends on the previous one: you can't extract fields from unparsed XML, and you can't convert to JSON without typed field values.

Without orchestration, you'd write a single JAXB or DOM parser method that reads XML, extracts fields, builds JSON, and outputs records in one pass. If the field extraction logic breaks on an unexpected XML structure (say, a `price` element with currency attributes), the entire parse fails with no visibility into which element caused the issue. There's no record of how many elements were parsed vs, how many fields were extracted vs, how many records were emitted. Re-running the pipeline after fixing an extraction bug means re-parsing the entire XML file from scratch.

## The Solution

**You just write the XML reception, tag parsing, field extraction, JSON conversion, and record emission workers. Conductor handles the multi-stage XML pipeline, retries when parsing fails on unexpected structures, and element-by-element count tracking across every stage.**

Each stage of the XML pipeline is a simple, independent worker. The receiver accepts the raw XML content and the root element name, validating that the XML is well-formed. The tag parser walks the XML structure and extracts elements under the specified root tag, producing a list of parsed elements. The field extractor pulls typed values from each element: strings for `id`, `name`, and `category`, doubles for `price`, producing clean records. The JSON converter transforms the extracted records into JSON format, adding metadata like source identifier and `parsedAt` timestamp to each record. The emitter produces the final record set with a completion status and record count. Conductor executes them in strict sequence, passes the evolving data representation between stages, retries if any step fails, and tracks element counts and record counts at every stage. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle XML-to-JSON transformation: receiving raw XML content, parsing tags into structured elements under the root, extracting typed fields (strings, doubles) from each element, converting to JSON records with metadata, and emitting the final dataset.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertToJsonWorker** | `xp_convert_to_json` | Converts extracted data to JSON records by adding source and parsedAt metadata. |
| **EmitRecordsWorker** | `xp_emit_records` | Emits the final parsed records with a completion status. |
| **ExtractFieldsWorker** | `xp_extract_fields` | Extracts id, name, price (as double), and category from each parsed element. |
| **ParseTagsWorker** | `xp_parse_tags` | Parses XML tags and returns simulated parsed elements. |
| **ReceiveXmlWorker** | `xp_receive_xml` | Receives raw XML content and passes it along with metadata. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== XML Parsing Workflow Demo ===

Step 1: Registering task definitions...
  Registered: xp_receive_xml, xp_parse_tags, xp_extract_fields, xp_convert_to_json, xp_emit_records

Step 2: Registering workflow 'xml_parsing_wf'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [xp_convert_to_json] Converting
  [xp_emit_records] Emitting
  [xp_extract_fields] Extracting fields from
  [xp_parse_tags] Parsing tags from root:
  [xp_receive_xml] Received XML with root:

  Status: COMPLETED
  Output: {source=..., parsedAt=..., jsonRecords=..., count=...}

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
java -jar target/xml-parsing-1.0.0.jar
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
java -jar target/xml-parsing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow xml_parsing_wf \
  --version 1 \
  --input '{"xmlContent": "Sample xmlContent", "><name>Laptop</name></product></products>": "sample-name", "rootElement": "sample-rootElement"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w xml_parsing_wf -s COMPLETED -c 5
```

## How to Extend

Parse real partner XML catalogs with JAXB or StAX, extract typed fields with namespace-aware processing, and publish JSON records to a downstream API, the XML-to-JSON workflow runs unchanged.

- **ReceiveXmlWorker** → receive XML from real sources: S3/GCS file downloads, SFTP pulls from partner systems, webhook payloads from EDI gateways, or Kafka messages containing XML payloads, with XML well-formedness validation using SAX parser
- **ParseTagsWorker** → implement real XML parsing using JAXB for schema-bound parsing, StAX for memory-efficient streaming of large XML files, or DOM for small documents where XPath queries are needed; support namespaces and configurable root element paths
- **ExtractFieldsWorker** → extract real typed fields with validation: parse dates using configurable date formats, handle currency-annotated price elements, support nested field extraction (address/city, address/zip), and apply XSD type constraints
- **ConvertToJsonWorker** → use real JSON serialization with Jackson or Gson, apply configurable field naming strategies (camelCase, snake_case), flatten nested XML structures into dot-notation JSON keys, and attach provenance metadata (source file, parse timestamp, schema version)
- **EmitRecordsWorker** → publish records to real downstream systems: REST API bulk endpoints, Kafka topics for event-driven consumers, database bulk inserts via JDBC batch, or Elasticsearch bulk index API

Replacing the tag parser with a real JAXB or SAX implementation or adding new typed fields requires only worker changes, the receive-parse-extract-convert-emit pipeline remains intact.

**Add new stages** by inserting tasks in `workflow.json`, for example, an XSD validation step that checks the XML against a schema before parsing, a deduplication step that filters out products already in the catalog, or a transformation step that enriches extracted records with data from a reference database (supplier names, category hierarchies).

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
xml-parsing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/xmlparsing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── XmlParsingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConvertToJsonWorker.java
│       ├── EmitRecordsWorker.java
│       ├── ExtractFieldsWorker.java
│       ├── ParseTagsWorker.java
│       └── ReceiveXmlWorker.java
└── src/test/java/xmlparsing/workers/
    ├── ConvertToJsonWorkerTest.java        # 9 tests
    ├── EmitRecordsWorkerTest.java        # 9 tests
    ├── ExtractFieldsWorkerTest.java        # 9 tests
    ├── ParseTagsWorkerTest.java        # 8 tests
    └── ReceiveXmlWorkerTest.java        # 9 tests
```
