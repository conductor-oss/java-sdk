# Search Agent: Formulate, Parallel Google/Wiki Search, Rank, Synthesize

The agent formulates three query variants by splitting and recombining the question words. Two searchers run in parallel via FORK_JOIN: Google (using a `GOOGLE_RESULTS_POOL` with titles like "Quantum Computing Reaches New Milestone") and Wikipedia (using `WIKI_RESULTS_POOL`). Results are ranked/merged (null-safe), and the synthesizer produces an answer with confidence score, sources used, and answer type.

## Workflow

```
question, maxResults
  -> sa_formulate_queries
  -> FORK_JOIN(sa_search_google | sa_search_wiki)
  -> sa_rank_merge -> sa_synthesize
```

## Tests

42 tests cover query formulation, both search sources, ranking, and synthesis.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
