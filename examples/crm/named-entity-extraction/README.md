# Named Entity Extraction in Java with Conductor :  Tokenize, Tag, Extract, and Link Entities from Text

A Java Conductor workflow that extracts named entities from text .  tokenizing the input into words, tagging each token with part-of-speech and entity labels, extracting recognized entities (persons, organizations, locations, dates), and linking those entities to knowledge base records. Given raw `text`, the pipeline produces tokens, tagged sequences, extracted entities, and linked references. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step NER pipeline.

## Finding the People, Places, and Organizations in Unstructured Text

Unstructured text .  emails, support tickets, news articles, legal documents ,  contains valuable structured information buried in natural language. Extracting entities like person names, company names, locations, and dates transforms text into actionable data. But NER is a pipeline: you need to tokenize the text into words, tag each word with its grammatical role and entity type, group tagged words into complete entities, and resolve those entities against a knowledge base to disambiguate (e.g., "Apple" the company vs. "apple" the fruit).

This workflow processes text through the full NER pipeline. The tokenizer splits text into individual tokens. The tagger labels each token with part-of-speech tags and entity type indicators (B-PER, I-ORG, etc.). The entity extractor groups tagged tokens into complete entity spans. The linker resolves extracted entities against a knowledge base, matching "Microsoft" to its canonical record and "Seattle" to its geographic entry.

## The Solution

**You just write the tokenization, tagging, entity-extraction, and linking workers. Conductor handles the NER pipeline sequencing and token flow.**

Four workers form the NER pipeline .  tokenization, tagging, entity extraction, and entity linking. The tokenizer splits raw text into word tokens. The tagger assigns part-of-speech and entity labels. The extractor groups labeled tokens into entity spans. The linker resolves entities against a knowledge base. Conductor sequences the four steps and passes tokens, tags, and entity lists between them via JSONPath.

### What You Write: Workers

TokenizeWorker splits text into words, TagWorker assigns entity labels like B-PER and I-ORG, ExtractEntitiesWorker groups tagged tokens into entity spans, and LinkWorker resolves them against a knowledge base.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractEntitiesWorker** | `ner_extract_entities` | Groups tagged tokens into complete entity spans (e.g., "Microsoft Corporation" as one ORG entity). |
| **LinkWorker** | `ner_link` | Resolves extracted entities against a knowledge base, matching names to canonical records. |
| **TagWorker** | `ner_tag` | Labels each token with part-of-speech and entity type indicators (B-PER, I-ORG, B-LOC, etc.). |
| **TokenizeWorker** | `ner_tokenize` | Splits the raw input text into individual word tokens. |

Workers simulate CRM operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ner_tokenize
    │
    ▼
ner_tag
    │
    ▼
ner_extract_entities
    │
    ▼
ner_link
```

## Example Output

```
=== Example 635: Named Entity Extractio ===

Step 1: Registering task definitions...
  Registered: ner_tokenize, ner_tag, ner_extract_entities, ner_link

Step 2: Registering workflow 'ner_named_entity_extraction'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [extract] Extracted
  [link]
  [tag]
  [tokenize] Text tokenized into

  Status: COMPLETED
  Output: {entities=..., entityCount=..., wikiId=..., url=...}

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
java -jar target/named-entity-extraction-1.0.0.jar
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
java -jar target/named-entity-extraction-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ner_named_entity_extraction \
  --version 1 \
  --input '{"text": "Sample text"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ner_named_entity_extraction -s COMPLETED -c 5
```

## How to Extend

Each worker handles one NER stage .  connect your NLP service (spaCy, Stanford NER, AWS Comprehend) for tagging and your knowledge graph (Wikidata, internal KB) for entity linking, and the extraction workflow stays the same.

- **ExtractEntitiesWorker** (`ner_extract_entities`): use spaCy, Stanford NER, or an LLM for production-grade entity extraction
- **LinkWorker** (`ner_link`): connect to Wikidata, DBpedia, or your CRM's contact database for real entity resolution
- **TagWorker** (`ner_tag`): integrate with Hugging Face transformer models (BERT NER) for more accurate token classification

Plug in a real NER model (spaCy, Hugging Face) and the tokenize-tag-extract-link entity pipeline operates unchanged.

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
named-entity-extraction/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/namedentityextraction/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NamedEntityExtractionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractEntitiesWorker.java
│       ├── LinkWorker.java
│       ├── TagWorker.java
│       └── TokenizeWorker.java
└── src/test/java/namedentityextraction/workers/
    ├── ExtractEntitiesWorkerTest.java        # 2 tests
    ├── LinkWorkerTest.java        # 2 tests
    ├── TagWorkerTest.java        # 2 tests
    └── TokenizeWorkerTest.java        # 2 tests
```
