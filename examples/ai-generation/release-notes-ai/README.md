# Release Notes AI in Java with Conductor : Generate Release Notes from Git Commits Between Tags

## Writing Release Notes That No One Wants to Write

Release notes are essential for users but tedious for developers. Manually scanning git log output, deciding which commits matter, grouping them into categories, and writing user-facing descriptions takes time that developers would rather spend coding. The information is already in the commit history. it just needs to be extracted, organized, and rewritten for a non-developer audience.

This workflow automates release note generation. The commit collector gathers all commits between the `fromTag` and `toTag`. The categorizer labels each commit as a feature, bugfix, improvement, or breaking change. The notes generator transforms the categorized commits into polished, user-facing release notes grouped by category. The publisher posts the generated notes to the appropriate platform. Each step's output feeds the next. commits feed categorization, categories feed note generation, and generated notes feed publishing.

## The Solution

**You just write the commit-collection, categorization, notes-generation, and publishing workers. Conductor handles the release-notes pipeline and tag-range data flow.**

Four workers handle the release notes pipeline. commit collection, categorization, note generation, and publishing. The collector pulls commits between tags. The categorizer labels each commit by type. The generator produces human-readable notes grouped by category. The publisher posts the final document. Conductor sequences the four steps and passes commit lists, categories, and formatted notes between them via JSONPath.

### What You Write: Workers

CollectCommitsWorker gathers commits between tags, CategorizeWorker labels each as feature/bugfix/improvement, GenerateNotesWorker transforms them into user-facing copy, and PublishWorker posts the final document.

| Worker | Task | What It Does |
|---|---|---|
| **CategorizeWorker** | `rna_categorize` | Labels each commit as a feature, bugfix, improvement, or breaking change. |
| **CollectCommitsWorker** | `rna_collect_commits` | Gathers all commits between the fromTag and toTag from the repository. |
| **GenerateNotesWorker** | `rna_generate_notes` | Transforms categorized commits into polished, user-facing release notes grouped by category. |
| **PublishWorker** | `rna_publish` | Publishes the generated release notes to a configured platform. |

### The Workflow

```
rna_collect_commits
 │
 ▼
rna_categorize
 │
 ▼
rna_generate_notes
 │
 ▼
rna_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
