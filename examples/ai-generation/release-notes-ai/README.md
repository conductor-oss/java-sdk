# Generate Release Notes from Git Commits Between Tags

Collect commits between tags (e.g., `{hash: "a1b2c3", message: "feat: add user dashboard", author: "alice"}`), categorize them (features, fixes, etc.), generate formatted release notes, and publish.

## Workflow

```
repoName, fromTag, toTag
  -> rna_collect_commits -> rna_categorize -> rna_generate_notes -> rna_publish
```

## Tests

8 tests cover commit collection, categorization, note generation, and publishing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
