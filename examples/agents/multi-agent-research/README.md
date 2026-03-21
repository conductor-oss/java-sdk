# Multi-Agent Research in Java Using Conductor: Define, Parallel Search Across Web/Papers/Databases, Synthesize, Report

Your research intern searches Google, finds three blog posts, and writes the report. No academic papers. No internal data. The conclusions sound confident but rest on a single source type. Another intern starts from Semantic Scholar, finds contradicting peer-reviewed evidence, but never cross-references the web findings. When research agents work sequentially on one source at a time, a five-minute task takes an hour, and you still get a biased report. This example fans out three specialized search agents (web, academic papers, internal databases) in parallel using Conductor's `FORK_JOIN`, then synthesizes and cross-references their findings into a single report. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Comprehensive Research Requires Multiple Sources

Researching a topic from a single source produces biased, incomplete findings. Web search gives you current news and opinions. Academic papers provide peer-reviewed evidence. Databases contain structured data and statistics. A comprehensive research report draws from all three, cross-referencing web findings with academic evidence and database statistics.

The three search types are independent. Web search doesn't depend on paper search, so they should run simultaneously. After all three complete, the synthesizer must merge findings, resolve contradictions between sources, and identify where multiple sources agree (high confidence) or disagree (needs further investigation). The report writer then structures everything into a coherent document with citations.

## The Solution

**You write the search, synthesis, and report generation logic. Conductor handles parallel source querying, cross-reference merging, and citation tracking.**

`DefineResearchWorker` establishes the research scope, key questions, and evaluation criteria. `FORK_JOIN` dispatches three search agents in parallel: `SearchWebWorker` finds current articles, blog posts, and news. `SearchPapersWorker` queries academic databases for peer-reviewed research. `SearchDatabasesWorker` retrieves relevant structured data and statistics. After `JOIN` collects all results, `SynthesizeWorker` merges findings across sources, resolves contradictions, and identifies consensus. `WriteReportWorker` structures the synthesis into a formatted research report with citations and confidence levels. Conductor runs all three searches simultaneously and tracks which sources contributed to each finding.

### What You Write: Workers

Six workers conduct the research. Defining scope, searching web, papers, and databases in parallel, synthesizing findings, and writing the report.

| Worker | Task | What It Does |
|---|---|---|
| **DefineResearchWorker** | `ra_define_research` | Defines the research scope. Takes a topic and depth, produces search queries, target domains, and database names to .. |
| **SearchDatabasesWorker** | `ra_search_databases` | Searches internal databases for proprietary findings. Takes queries and database names, returns findings with source.. |
| **SearchPapersWorker** | `ra_search_papers` | Searches academic papers for scholarly findings. Takes queries and academic domains, returns findings with citations.. |
| **SearchWebWorker** | `ra_search_web` | Searches the web for relevant findings. Takes queries and maxResults, returns a list of findings with source, title,.. |
| **SynthesizeWorker** | `ra_synthesize` | Synthesizes findings from all three search agents. Computes total sources, average credibility, produces a synthesis.. |
| **WriteReportWorker** | `ra_write_report` | Writes the final research report. Takes topic, synthesis, key insights, and source count, and produces a title, exec.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
ra_define_research
 │
 ▼
FORK_JOIN
 ├── ra_search_web
 ├── ra_search_papers
 └── ra_search_databases
 │
 ▼
JOIN (wait for all branches)
ra_synthesize
 │
 ▼
ra_write_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
