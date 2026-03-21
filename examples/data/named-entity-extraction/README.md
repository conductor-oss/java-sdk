# Named Entity Extraction in Java with Conductor : Tokenize, Tag, Extract, and Link Entities from Text

## Finding the People, Places, and Organizations in Unstructured Text

Unstructured text. emails, support tickets, news articles, legal documents, contains valuable structured information buried in natural language. Extracting entities like person names, company names, locations, and dates transforms text into actionable data. But NER is a pipeline: you need to tokenize the text into words, tag each word with its grammatical role and entity type, group tagged words into complete entities, and resolve those entities against a knowledge base to disambiguate (e.g., "Apple" the company vs. "apple" the fruit).

This workflow processes text through the full NER pipeline. The tokenizer splits text into individual tokens. The tagger labels each token with part-of-speech tags and entity type indicators (B-PER, I-ORG, etc.). The entity extractor groups tagged tokens into complete entity spans. The linker resolves extracted entities against a knowledge base, matching "Microsoft" to its canonical record and "Seattle" to its geographic entry.

## The Solution

**You just write the tokenization, tagging, entity-extraction, and linking workers. Conductor handles the NER pipeline sequencing and token flow.**

Four workers form the NER pipeline. tokenization, tagging, entity extraction, and entity linking. The tokenizer splits raw text into word tokens. The tagger assigns part-of-speech and entity labels. The extractor groups labeled tokens into entity spans. The linker resolves entities against a knowledge base. Conductor sequences the four steps and passes tokens, tags, and entity lists between them via JSONPath.

### What You Write: Workers

TokenizeWorker splits text into words, TagWorker assigns entity labels like B-PER and I-ORG, ExtractEntitiesWorker groups tagged tokens into entity spans, and LinkWorker resolves them against a knowledge base.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractEntitiesWorker** | `ner_extract_entities` | Groups tagged tokens into complete entity spans (e.g., "Microsoft Corporation" as one ORG entity). |
| **LinkWorker** | `ner_link` | Resolves extracted entities against a knowledge base, matching names to canonical records. |
| **TagWorker** | `ner_tag` | Labels each token with part-of-speech and entity type indicators (B-PER, I-ORG, B-LOC, etc.). |
| **TokenizeWorker** | `ner_tokenize` | Splits the raw input text into individual word tokens. |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
