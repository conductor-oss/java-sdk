# Documentation AI in Java with Conductor : Auto-Generate Docs from Source Code

## Keeping Documentation in Sync with Code

Documentation drifts out of sync with code because writing it is manual and updating it is an afterthought. Developers change function signatures, add modules, and refactor classes; but the docs stay frozen at the last time someone remembered to update them. Automating documentation generation from the source code itself eliminates this drift.

This workflow reads a codebase and produces publishable documentation. The code analyzer scans the repository to discover modules, classes, functions, and their relationships. The doc generator produces raw documentation pages for each module. The formatter converts the raw docs into the requested output format (Markdown, HTML, or PDF). The publisher uploads the formatted docs to a hosting platform and returns the URL. Each step builds on the previous one. you cannot format docs that have not been generated, and you cannot generate docs without first understanding the code structure.

## The Solution

**You just write the code-analysis, doc-generation, formatting, and publishing workers. Conductor handles the pipeline sequencing and output routing.**

Four workers handle the documentation lifecycle. code analysis, doc generation, formatting, and publishing. The analyzer discovers modules and their structure. The generator creates documentation pages for each module. The formatter renders the pages in the requested output format. The publisher deploys the formatted docs and returns a URL. Conductor sequences the pipeline and passes module lists, raw docs, and formatted docs between steps via JSONPath.

### What You Write: Workers

AnalyzeCodeWorker discovers modules and function signatures, GenerateDocsWorker produces documentation pages, FormatWorker renders them in Markdown or HTML, and PublishWorker deploys to the hosting platform.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeCodeWorker** | `doc_analyze_code` | Scans the repository to discover modules, classes, and function signatures. |
| **FormatWorker** | `doc_format` | Converts raw documentation into the requested output format (Markdown, HTML, or PDF). |
| **GenerateDocsWorker** | `doc_generate_docs` | Generates documentation pages for each discovered module with descriptions and usage examples. |
| **PublishWorker** | `doc_publish` | Uploads formatted documentation to a hosting platform and returns the published URL. |

### The Workflow

```
doc_analyze_code
 │
 ▼
doc_generate_docs
 │
 ▼
doc_format
 │
 ▼
doc_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
