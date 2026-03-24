# Automated PR Review: Fetch Diff, Analyze, Generate Review, Post

Fetch the PR diff (`files: ["src/auth.js", "src/api.js", "tests/auth.test.js"]`), analyze for issues (`{file: "src/auth.js", line: 42, type: "security", message: "Potential SQL injection"}`), generate a review with severity-based counts, and post the review.

## Workflow

```
repoName, prNumber
  -> prr_fetch_diff -> prr_analyze_changes -> prr_generate_review -> prr_post_review
```

## Tests

8 tests cover diff fetching, analysis, review generation, and posting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
