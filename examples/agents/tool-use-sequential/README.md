# Sequential Tool Use in Java Using Conductor : Search, Read Page, Extract Data, Summarize

Tool Use Sequential: search the web, read a page, extract data, and summarize through a sequential pipeline of demo tool calls. ## Some Tools Need to Be Called in Order

"Find the latest quarterly revenue for Acme Corp and summarize the key drivers" requires four sequential steps: search the web for Acme Corp quarterly results, read the most relevant page (likely an earnings report or press release), extract the revenue figures and key metrics from the page content, and summarize the findings in natural language.

Each step depends on the previous one. you can't read a page without a URL from search results, you can't extract data without page content, and you can't summarize without extracted data. If the page read fails (timeout, paywall), you need to retry without re-running the search. And you want to see each step's output: which URL was selected, what content was extracted, what data was found.

## The Solution

**You write the search, page reading, data extraction, and summarization logic. Conductor handles the sequential tool chain, per-step retries, and full research trail recording.**

`SearchWebWorker` queries a search engine and returns ranked results with URLs, titles, and snippets. `ReadPageWorker` fetches the top result URL and extracts the page content (text, tables, metadata). `ExtractDataWorker` parses the page content to extract structured data. numbers, dates, entities, relationships. `SummarizeWorker` generates a natural language summary from the extracted data. Conductor chains these four tools in sequence, passing each output to the next, and records the full chain for debugging.

### What You Write: Workers

Four workers chain tool calls in order. Searching the web, reading the top page, extracting structured data, and summarizing the findings.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractDataWorker** | `ts_extract_data` | Simulates extracting structured data from page content. Takes pageContent and query, returns extracted facts, key fea... |
| **ReadPageWorker** | `ts_read_page` | Simulates reading a web page. Takes a url and title, returns the page content including sections and word count. |
| **SearchWebWorker** | `ts_search_web` | Simulates a web search tool. Takes a query and maxResults, returns a list of search results with url, title, and snip... |
| **SummarizeWorker** | `ts_summarize` | Simulates summarizing extracted data into a coherent summary. Takes query, extractedData, sourceUrl, and sourceTitle.... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ts_search_web
 │
 ▼
ts_read_page
 │
 ▼
ts_extract_data
 │
 ▼
ts_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
