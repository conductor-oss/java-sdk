# Researcher-Writer-Reviewer Pipeline

Three agents work in sequence: the researcher gathers key facts and statistics, the writer uses `research.get("keyFacts")` and `research.get("statistics")` (null-safe via ternary) to produce a draft, the reviewer provides suggestions and a score, and the final output compiles everything including `reviewScore`.

## Workflow

```
subject, audience
  -> thr_researcher_agent -> thr_writer_agent -> thr_reviewer_agent -> thr_final_output
```

## Tests

33 tests cover research, writing with null-safe data access, review scoring, and final compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
