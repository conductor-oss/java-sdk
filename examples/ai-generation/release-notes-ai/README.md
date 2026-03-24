# Release Notes AI: From Git Commits to Published Release Notes

A release is ready to ship, containing 30+ commits between the last tag and the current one. The release manager needs to write release notes that group changes by category (features, bug fixes, improvements, breaking changes), are readable by end users who do not understand commit messages like "fix: handle null ptr in auth middleware", and are published before the release goes live. Doing this manually for every release is tedious and inconsistent.

This workflow collects commits between two tags, categorizes them, generates formatted release notes, and publishes the result.

## Pipeline Architecture

```
repoName, fromTag, toTag
         |
         v
  rna_collect_commits    (commits list, commitCount)
         |
         v
  rna_categorize         (categories map: features, fixes, improvements, etc.)
         |
         v
  rna_generate_notes     (releaseNotes, sectionCount)
         |
         v
  rna_publish            (published=true, version from toTag)
```

## Worker: CollectCommits (`rna_collect_commits`)

Gathers all commits between `fromTag` and `toTag`. Returns a `commits` list where each entry is a map with `hash`, `message`, and `author` fields (e.g., `{hash: "a1b2c3", message: "feat: add user dashboard", author: "alice"}`). Reports `commitCount` equal to `commits.size()`.

## Worker: Categorize (`rna_categorize`)

Classifies each commit into categories based on the commit message prefix convention (feat, fix, chore, docs, etc.). Returns a `categories` map where keys are category names (features, fixes, improvements) and values are lists of the commits belonging to each category. Uncategorized commits go into a miscellaneous bucket.

## Worker: GenerateNotes (`rna_generate_notes`)

Transforms the categorized commits into human-readable release notes. Returns `releaseNotes` with the formatted content organized by category, and `sectionCount` indicating the number of non-empty categories. Each section contains user-facing descriptions derived from the raw commit messages.

## Worker: Publish (`rna_publish`)

Publishes the release notes. Returns `published: true` and `version` set to the `toTag` value, confirming that the notes are now associated with the correct release version.

## Tests

4 tests cover commit collection, categorization, note generation, and publishing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
