# Multi-Agent Content Creation in Java Using Conductor : Research, Write, SEO Optimize, Edit, Publish

Multi-Agent Content Creation. research, write, optimize SEO, edit, and publish content through a sequential pipeline of specialized agents. ## Quality Content Requires Specialized Roles

Asking an LLM to "write a blog post about Kubernetes" in a single call produces generic, unresearched content with no SEO optimization and no editorial review. Quality content creation mirrors a real editorial workflow: a researcher gathers facts and sources, a writer crafts the narrative for a specific audience and word count, an SEO specialist adds keywords and meta descriptions and optimizes headings, an editor improves readability and catches errors, and a publisher formats and delivers the final piece.

Each agent builds on the previous one's output. the writer uses the researcher's sources, the SEO agent works with the writer's draft, and the editor reviews the SEO-optimized version. If the SEO optimization step produces keyword-stuffed prose, the editor catches it. If the editor's changes break SEO titles, the workflow can be extended with a verification step. Without orchestration, this pipeline becomes a single prompt that tries to do everything at once and does nothing well.

## The Solution

**You write the research, writing, SEO, editing, and publishing logic. Conductor handles the editorial pipeline, version tracking, and content delivery.**

`ResearchAgentWorker` gathers source material, key facts, and statistics on the topic for the target audience. `WriterAgentWorker` drafts the article using the research, targeting the specified word count and audience level. `SeoAgentWorker` optimizes the draft for search. adding keyword density, meta descriptions, alt text, internal linking suggestions, and heading optimization. `EditorAgentWorker` reviews for grammar, style, readability, and factual consistency. `PublishWorker` formats the final content and delivers it to the specified channel. Conductor chains these five agents and records each version of the content (draft, SEO-optimized, edited, published) for editorial tracking.

### What You Write: Workers

Five agents form the editorial pipeline. Researching the topic, writing the draft, optimizing for SEO, editing for quality, and publishing the final piece.

| Worker | Task | What It Does |
|---|---|---|
| **EditorAgentWorker** | `cc_editor_agent` | Editor agent. polishes the SEO-optimized article, computes metadata, and determines readability grade. Takes article.. |
| **PublishWorker** | `cc_publish` | Publish worker. publishes the final article with metadata. Takes finalArticle, metadata, and seoScore; returns url.. |
| **ResearchAgentWorker** | `cc_research_agent` | Research agent. gathers facts and sources on a given topic for the target audience. Returns 4 facts, 3 sources, and .. |
| **SeoAgentWorker** | `cc_seo_agent` | SEO agent. optimizes an article draft for search engines. Takes draft and topic; returns optimizedArticle, 4 suggest.. |
| **WriterAgentWorker** | `cc_writer_agent` | Writer agent. composes an article draft from research facts and sources. Takes topic, facts, sources, and wordCount;.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cc_research_agent
 │
 ▼
cc_writer_agent
 │
 ▼
cc_seo_agent
 │
 ▼
cc_editor_agent
 │
 ▼
cc_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
