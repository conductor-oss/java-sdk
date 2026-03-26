# Named Entity Extraction

A legal document processing system receives contracts in plain text. Each document needs tokenization, named-entity recognition (people, organizations, dates, monetary amounts), and structured extraction into a JSON schema that downstream contract-management software can ingest.

## Pipeline

```
[ner_tokenize]
     |
     v
[ner_tag]
     |
     v
[ner_extract_entities]
     |
     v
[ner_link]
```

**Workflow inputs:** `text`

## Workers

**ExtractEntitiesWorker** (task: `ner_extract_entities`)

- Writes `entities`, `entityCount`

**LinkWorker** (task: `ner_link`)

- Uses randomization
- Writes `linkedEntities`

**TagWorker** (task: `ner_tag`)

- Filters with predicates
- Writes `taggedTokens`

**TokenizeWorker** (task: `ner_tokenize`)

- Writes `tokens`, `tokenCount`

---

**8 tests** | Workflow: `ner_named_entity_extraction` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
