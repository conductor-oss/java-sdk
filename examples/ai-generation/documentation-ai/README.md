# Auto-Generate Documentation from Source Code

Analyze a codebase to find modules (e.g., `{name: "auth", functions: 8, classes: 2}`), generate documentation per module (3 sections each), format the output, and publish to `"https://docs.example.com/v2"`.

## Workflow

```
repoPath, outputFormat
  -> doc_analyze_code -> doc_generate_docs -> doc_format -> doc_publish
```

## Tests

8 tests cover code analysis, doc generation, formatting, and publishing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
