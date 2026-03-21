# JSON Transformation in Java Using Conductor :  Field Mapping, Value Transforms, Nested Restructuring, and Schema Validation

A Java Conductor workflow example for JSON-to-JSON transformation: parsing an incoming JSON record, renaming fields according to mapping rules (`cust_id` to `customerId`, `first_name` + `last_name` to `fullName`), applying value transforms (lowercasing emails, uppercasing account types, concatenating name fields), restructuring flat fields into nested groups (`identity`, `contact`, `account`), validating that the output conforms to the target schema, and emitting the final transformed record. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your upstream system sends customer records with snake_case field names (`cust_id`, `first_name`, `last_name`, `acct_type`, `reg_date`) in a flat structure, but your downstream API expects camelCase fields (`customerId`, `fullName`, `accountType`) organized into nested groups (`identity.id`, `contact.email`, `account.type`). The transformation is not just renaming. `first_name` and `last_name` need to be concatenated into `fullName` with whitespace trimmed, `email` needs to be lowercased for consistency, and `acct_type` needs to be uppercased to match the enum values the downstream system expects. After restructuring, the output must conform to a target schema: `identity.id` and `contact.email` are required fields, and any record missing them is invalid.

Without orchestration, you'd write a single mapper method that parses, renames, transforms, restructures, and validates in one pass. When a field rename breaks the schema validation, you'd have no visibility into whether the issue was in the mapping rules, the restructuring logic, or the source data. If you want to add a new transform (say, phone number formatting), you'd modify deeply coupled code. There's no record of what the intermediate mapped record looked like before restructuring, making it impossible to debug transform chain issues.

## The Solution

**You just write the JSON parsing, field mapping, nested restructuring, schema validation, and output workers. Conductor handles the multi-stage transformation sequence, retries on step failures, and field count tracking at every stage so you can trace how the record shape evolves from input to output.**

Each stage of the JSON transformation is a simple, independent worker. The parser reads the source JSON and counts its top-level fields. The field mapper applies the mapping rules. Renaming `cust_id` to `customerId`, concatenating `first_name` and `last_name` into `fullName`, lowercasing the email, and uppercasing the account type. The restructurer takes the flat mapped record and organizes it into nested groups (`identity`, `contact`, `account`). The schema validator checks that required fields like `identity.id` and `contact.email` exist in the restructured output. The emitter produces the final record for downstream consumption. Conductor executes them in sequence, passes the evolving record between stages, retries if a step fails, and tracks the field count at every stage so you can see exactly how the record shape changed from input to output. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle JSON transformation: parsing the source JSON, mapping and renaming fields with value transforms, restructuring flat fields into nested groups, validating against the target schema, and emitting the final record.

| Worker | Task | What It Does |
|---|---|---|
| **EmitOutputWorker** | `jt_emit_output` | Emits the final transformed record. |
| **MapFieldsWorker** | `jt_map_fields` | Maps source fields to target fields with renaming and value transforms. |
| **ParseInputWorker** | `jt_parse_input` | Parses the incoming source JSON and counts its top-level fields. |
| **RestructureNestedWorker** | `jt_restructure_nested` | Restructures a flat mapped record into nested groups. |
| **ValidateSchemaWorker** | `jt_validate_schema` | Validate Schema. Computes and returns validated, is valid |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
jt_parse_input
    │
    ▼
jt_map_fields
    │
    ▼
jt_restructure_nested
    │
    ▼
jt_validate_schema
    │
    ▼
jt_emit_output

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
java -jar target/json-transformation-1.0.0.jar

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
java -jar target/json-transformation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow json_transformation \
  --version 1 \
  --input '{"sourceJson": "api", "mappingRules": "sample-mappingRules"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w json_transformation -s COMPLETED -c 5

```

## How to Extend

Load mapping rules from a configuration store, validate against JSON Schema Draft 2020-12, and publish transformed records to Kafka or a downstream API, the transformation workflow runs unchanged.

- **ParseInputWorker** → parse real JSON payloads from webhook bodies, Kafka messages, or S3 objects, handling malformed JSON gracefully and supporting multiple input encodings (UTF-8, ISO-8859-1)
- **MapFieldsWorker** → load mapping rules from a configuration store (database, YAML file, or admin UI) so field mappings can be updated without code changes; support JOLT transforms or JSONata expressions for advanced mappings
- **RestructureNestedWorker** → implement configurable nesting strategies using JSONPath or dot-notation path definitions (`contact.email`, `identity.id`) so the target structure can be changed via configuration
- **ValidateSchemaWorker** → validate against real JSON Schema (Draft 2020-12) definitions, supporting required fields, type constraints, pattern matching, and custom format validators for emails, phone numbers, and dates
- **EmitOutputWorker** → publish the transformed record to a downstream API, Kafka topic, database table, or message queue, with content-type negotiation and response status tracking

Adding new field mappings or adjusting the nested grouping structure requires only worker-level changes, the parse-map-restructure-validate-emit pipeline remains the same.

**Add new stages** by inserting tasks in `workflow.json`, for example, a field enrichment step that looks up missing values from a reference database, a deduplication step that checks if this record was already transformed, or a batch aggregation step that collects transformed records and writes them in bulk.

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
json-transformation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/jsontransformation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── JsonTransformationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmitOutputWorker.java
│       ├── MapFieldsWorker.java
│       ├── ParseInputWorker.java
│       ├── RestructureNestedWorker.java
│       └── ValidateSchemaWorker.java
└── src/test/java/jsontransformation/workers/
    ├── EmitOutputWorkerTest.java        # 8 tests
    ├── MapFieldsWorkerTest.java        # 10 tests
    ├── ParseInputWorkerTest.java        # 8 tests
    ├── RestructureNestedWorkerTest.java        # 8 tests
    └── ValidateSchemaWorkerTest.java        # 9 tests

```
