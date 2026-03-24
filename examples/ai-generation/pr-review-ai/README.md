# PR Review AI: Automated Pull Request Analysis

A development team merges dozens of pull requests per week. Each PR should be reviewed for security issues, code quality, and adherence to standards before merging. Human reviewers are thorough but slow, and they fatigue on high-volume days, missing issues in the 15th PR that they would have caught in the 1st. An automated first-pass review catches the obvious issues and lets humans focus on architectural decisions and business logic.

This workflow fetches the PR diff, analyzes changes for issues, generates a structured review with severity-based scoring, and posts the review.

## Pipeline Architecture

```
repoName, prNumber
         |
         v
  prr_fetch_diff         (diff text, filesChanged count)
         |
         v
  prr_analyze_changes    (analysis with issue list, issueCount)
         |
         v
  prr_generate_review    (review with comments and verdict)
         |
         v
  prr_post_review        (posted=true)
```

## Worker: FetchDiff (`prr_fetch_diff`)

Fetches the PR diff from the repository. Returns `diff` (the raw diff text) and `filesChanged` reflecting the number of modified files (e.g., `files: ["src/auth.js", "src/api.js", "tests/auth.test.js"]`). The file list feeds into the change analyzer.

## Worker: AnalyzeChanges (`prr_analyze_changes`)

Scans the diff for potential issues. Returns an `analysis` map containing a list of findings, each with `file`, `line`, `type`, and `message` fields. For example: `{file: "src/auth.js", line: 42, type: "security", message: "Potential SQL injection"}`. Reports `issueCount` equal to the number of findings.

## Worker: GenerateReview (`prr_generate_review`)

Produces a structured review from the analysis. Returns a `review` map with `comments` (the list of issues with line-level annotations) and a `verdict` string. The verdict is determined by the severity distribution -- high-severity findings trigger a "request changes" verdict while low-severity findings result in an approval with comments.

## Worker: PostReview (`prr_post_review`)

Posts the generated review to the pull request. Returns `posted: true` confirming the review comment was submitted. The review includes all comments with their severity levels and line references.

## Tests

4 tests cover diff fetching, change analysis, review generation, and review posting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
