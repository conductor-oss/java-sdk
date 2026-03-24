# Documentation AI: Analyze Code, Generate Docs, Format, Publish

A codebase grows faster than its documentation. Developers add modules, classes, and functions but rarely update the corresponding docs. After a few sprints the documentation is stale, incomplete, or missing entirely. Manually auditing the codebase, writing documentation for each module, formatting it consistently, and publishing it to a docs site is a full-time job that nobody wants.

This workflow automates the full documentation lifecycle: analyze the code to discover modules, generate documentation per module, format the output, and publish.

## Pipeline Architecture

```
repoPath, outputFormat
         |
         v
  doc_analyze_code       (modules list, moduleCount)
         |
         v
  doc_generate_docs      (rawDocs, pageCount)
         |
         v
  doc_format             (formattedDocs, format)
         |
         v
  doc_publish            (publishUrl="https://docs.example.com/v2", published=true)
```

## Worker: AnalyzeCode (`doc_analyze_code`)

Scans the repository at `repoPath` and discovers modules. Returns a `modules` list where each entry contains metadata like `name: "auth"`, `functions: 8`, `classes: 2`. Also returns `moduleCount` equal to `modules.size()`. The module inventory drives the documentation generator.

## Worker: GenerateDocs (`doc_generate_docs`)

Generates documentation for each discovered module. Produces `rawDocs` containing section-by-section documentation (3 sections per module covering overview, API reference, and usage examples). Returns `pageCount` reflecting the total number of documentation pages generated across all modules.

## Worker: Format (`doc_format`)

Applies the requested `outputFormat` (e.g., Markdown, HTML) to the raw documentation. Returns `formattedDocs` (the styled content) and `format` confirming which format was applied. The formatted docs are ready for publishing.

## Worker: Publish (`doc_publish`)

Publishes the formatted documentation to the hosting platform. Returns `publishUrl: "https://docs.example.com/v2"` and `published: true`. The version in the URL (`v2`) indicates this is an update to existing documentation.

## Tests

4 tests cover code analysis, documentation generation, formatting, and publishing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
