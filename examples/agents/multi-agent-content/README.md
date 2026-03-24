# Multi-Agent Content Creation: Research, Write, SEO, Edit, Publish

A marketing team needs to produce a long-form article on a given topic for a specific audience. Writing it manually means one person has to research, draft, optimize for search engines, polish, and publish -- each step requiring different expertise. Missing any stage (especially SEO) means the article either lacks credibility or never surfaces in search results.

This workflow chains five specialized agents in sequence, where each agent consumes the previous agent's output and adds its own domain-specific processing.

## Pipeline Architecture

```
topic, targetAudience, wordCount
         |
         v
  cc_research_agent      (4 facts, 3 sources, topicRelevance=0.92)
         |
         v
  cc_writer_agent        (Markdown article with sections, draftWordCount)
         |
         v
  cc_seo_agent           (HTML comment injection, 4 suggestions, seoScore=87)
         |
         v
  cc_editor_agent        (polish, compute wordCount, readabilityGrade=8.2)
         |
         v
  cc_publish             (slug URL, publishedAt timestamp, status="live")
```

## Worker: ResearchAgent (`cc_research_agent`)

Generates four interpolated fact strings incorporating the `topic` parameter (e.g., "topic has seen a 40% increase in adoption over the past year") and three source citations. Returns a `topicRelevance` score fixed at `0.92`. Blank topic or audience inputs fall back to `"general topic"` and `"general audience"`.

## Worker: WriterAgent (`cc_writer_agent`)

Builds a full Markdown article with `# heading`, `## Key Findings`, `## Analysis`, `## Practical Implications`, `## Conclusion`, and `## References` sections. The target `wordCount` is parsed from either `Number` or `String` input, defaulting to `800`. Actual `draftWordCount` is computed via `split("\\s+").length`.

## Worker: SeoAgent (`cc_seo_agent`)

Appends an HTML comment block (`<!-- SEO optimized: ... -->`) to the draft. Returns four actionable suggestions: add meta description, increase keyword density from 1.2% to 2.0%, add 3 internal links, and include alt text. Outputs a fixed `seoScore` of `87`.

## Worker: EditorAgent (`cc_editor_agent`)

Appends editor notes referencing each SEO suggestion. Recomputes `wordCount` from the polished text. Calculates `readTime` deterministically as `ceil(wordCount / 200) + " min"`. Extracts the title from the first Markdown heading (`# ...`). Returns metadata as a `LinkedHashMap` with title, author, tags (`["technology", "innovation", "best-practices"]`), and readTime. Fixed `readabilityGrade` of `8.2`.

## Worker: Publish (`cc_publish`)

Generates a slug URL by lowercasing the title, replacing non-alphanumeric runs with hyphens, and stripping leading/trailing hyphens: `"https://content.example.com/articles/" + slug`. Returns `publishedAt` as `"2025-01-15T10:30:00Z"` and `status` as `"live"`.

## Tests

5 tests cover all five content pipeline stages.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
