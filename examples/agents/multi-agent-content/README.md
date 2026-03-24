# Content Pipeline: Research, Write, SEO Optimize, Edit, Publish

Five agents collaborate sequentially: the researcher finds 4 facts and 3 sources with a `topicRelevance` score. The writer produces a draft (word count via `split("\s+").length`). The SEO agent optimizes with keyword suggestions. The editor polishes (recounting words). The publisher deploys to `"https://content.example.com/articles/" + title` with the SEO score.

## Workflow

```
topic, targetAudience, wordCount
  -> cc_research_agent -> cc_writer_agent -> cc_seo_agent -> cc_editor_agent -> cc_publish
```

## Tests

43 tests cover all five content pipeline stages.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
