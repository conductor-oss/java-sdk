# PR Review AI in Java with Conductor : Fetch, Analyze, Review, and Post Comments on Pull Requests

## Giving Every PR a Thorough Review Automatically

Code reviews are essential but time-consuming. Reviewers miss things when they are tired, rushed, or unfamiliar with the codebase. An AI reviewer provides consistent, thorough feedback on every PR. catching patterns like missing error handling, security concerns, or style inconsistencies that humans might overlook. The AI review does not replace human reviewers; it gives them a head start by flagging issues before they even open the PR.

This workflow processes one pull request. The diff fetcher retrieves the PR changes from the repository. The change analyzer examines the diff for patterns, complexity, and potential issues. The review generator produces a structured review with specific comments tied to files and line numbers. The poster submits the review as comments on the PR. Each step's output feeds the next. the raw diff feeds analysis, the analysis feeds review generation, and the generated review feeds posting.

## The Solution

**You just write the diff-fetching, change-analysis, review-generation, and posting workers. Conductor handles the review pipeline and PR data flow.**

Four workers handle the review pipeline. diff fetching, change analysis, review generation, and review posting. The fetcher pulls the PR diff from the repository. The analyzer examines changes for patterns and issues. The generator creates review comments with file and line references. The poster submits the review to the PR. Conductor sequences the four steps and passes diffs, analyses, and review comments between them via JSONPath.

### What You Write: Workers

FetchDiffWorker retrieves the PR changes, AnalyzeChangesWorker examines patterns and complexity, GenerateReviewWorker produces line-level comments, and PostReviewWorker submits the review to the PR.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeChangesWorker** | `prr_analyze_changes` | Examines the parsed diff for code issues, complexity, and patterns; outputs an issue list and complexity rating. |
| **FetchDiffWorker** | `prr_fetch_diff` | Retrieves the PR diff from the repository, returning changed files, additions, and deletions. |
| **GenerateReviewWorker** | `prr_generate_review` | Produces a structured review with file- and line-level comments and an overall verdict (approve/request changes). |
| **PostReviewWorker** | `prr_post_review` | Posts the generated review as comments on the pull request. |

### The Workflow

```
prr_fetch_diff
 │
 ▼
prr_analyze_changes
 │
 ▼
prr_generate_review
 │
 ▼
prr_post_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
