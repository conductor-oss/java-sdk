# Web Browsing Agent in Java Using Conductor : Plan Search, Execute, Select Pages, Read, Extract Answer

Web Browsing Agent. plans search queries, executes searches, selects relevant pages, reads content, and extracts a synthesized answer. ## Answering Questions by Actually Reading Web Pages

Search engine snippets are often insufficient. "What are the specific system requirements for running Kubernetes 1.29 on bare metal?" requires actually reading the documentation page, not just the snippet. A web browsing agent goes deeper than search: it plans the right query, gets search results, selects which pages are most likely to contain the answer, reads the full page content, and extracts the specific information needed.

This is a five-step pipeline where each step narrows the focus: search returns many results, page selection picks the best few, reading gets the content, and extraction finds the specific answer. If page reading fails (timeout, JavaScript-heavy site), the agent can try the next selected page without re-running the search.

## The Solution

**You write the search planning, page selection, content reading, and answer extraction logic. Conductor handles the browsing pipeline, retries on page fetch failures, and full research trail recording.**

`PlanSearchWorker` analyzes the question and formulates an effective search query. adding specific keywords, removing ambiguity, and targeting authoritative sources. `ExecuteSearchWorker` runs the query and returns ranked results with URLs, titles, and snippets. `SelectPagesWorker` evaluates the results and selects the most promising pages based on source authority, snippet relevance, and content type. `ReadPageWorker` fetches and parses the selected page content. `ExtractAnswerWorker` finds the specific answer to the question within the page content and returns it with the source citation. Conductor chains these five steps and records the full research trail.

### What You Write: Workers

Five workers browse the web. Planning search queries, executing the search, selecting relevant pages, reading their content, and extracting the answer with citations.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteSearchWorker** | `wb_execute_search` | Executes search queries and returns demo search results. Returns a list of results with url, title, snippet, and... |
| **ExtractAnswerWorker** | `wb_extract_answer` | Extracts and synthesizes an answer from the page contents. Returns the answer, sources, confidence score, and word co... |
| **PlanSearchWorker** | `wb_plan_search` | Plans search queries for a given question. Returns a list of search queries, the search engine to use, and the strategy. |
| **ReadPageWorker** | `wb_read_page` | Reads the content of selected pages. Simulates page loading and content extraction. Returns page contents with url, t... |
| **SelectPagesWorker** | `wb_select_pages` | Selects the most relevant pages from search results based on relevance score. Sorts by relevance descending and retur... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
wb_plan_search
 │
 ▼
wb_execute_search
 │
 ▼
wb_select_pages
 │
 ▼
wb_read_page
 │
 ▼
wb_extract_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
