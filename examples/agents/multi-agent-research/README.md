# Parallel Research Across Web, Academic Papers, and Databases

A research topic is defined with search queries and target domains (`["computer science", "engineering", "business"]`). Three searchers work in parallel: web (finding sources like `techreview.com`), papers (academic domains), and databases. A synthesizer computes average credibility and produces key insights. A writer produces the report with executive summary.

## Workflow

```
topic, depth
  -> ra_define_research
  -> FORK_JOIN(ra_search_web | ra_search_papers | ra_search_databases)
  -> ra_synthesize -> ra_write_report
```

## Tests

55 tests cover research definition, all three search types, synthesis, and report writing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
