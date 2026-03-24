# Parallel Security, Quality, and Style Analysis of Pull Requests

A PR diff is parsed to extract changed files (`["src/auth.js", "src/api/users.js", "src/utils/crypto.js"]`). Three reviewers run in parallel via FORK_JOIN: security finds `{severity: "high", file: "src/utils/crypto.js", line: 23, message: "Weak hash algorithm (MD5)"}`, quality finds long functions, style finds missing JSDoc. A report compiles all findings.

## Workflow

```
prUrl, diff -> cra_parse_diff
  -> FORK_JOIN(cra_security_check | cra_quality_check | cra_style_check)
  -> cra_report
```

## Tests

10 tests cover diff parsing, all three review types, and report compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
