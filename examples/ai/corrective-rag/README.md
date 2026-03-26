# Self-Healing Retrieval: Falling Back to Web Search When Documents Are Irrelevant

A user asks a question that your vector store cannot answer well -- maybe the embeddings are stale or the topic isn't covered. Standard RAG generates from whatever comes back, relevant or not. This workflow adds a quality gate: it grades retrieved documents by relevance score, and if the average falls below 0.5, it abandons the vector store and falls back to a live Wikipedia search.

## Workflow

```
question
   │
   ▼
┌──────────────────┐
│ cr_retrieve_docs │  Jaccard similarity over bundled knowledge base
└────────┬─────────┘
         │  documents (top 3 with scores)
         ▼
┌────────────────────┐
│ cr_grade_relevance │  Average relevance >= 0.5?
└────────┬───────────┘
         │  verdict
         ▼
    ┌─ SWITCH ─────────────────────────────────┐
    │                                          │
  "relevant"                              default (irrelevant)
    │                                          │
    ▼                                          ▼
┌──────────────────┐                  ┌──────────────────┐
│ cr_generate_answer│                 │ cr_web_search    │
└──────────────────┘                  └────────┬─────────┘
                                               ▼
                                      ┌──────────────────────┐
                                      │ cr_generate_from_web │
                                      └──────────────────────┘
```

## Workers

**RetrieveDocsWorker** (`cr_retrieve_docs`) -- Searches a 6-document bundled `KNOWLEDGE_BASE` (topics: dynamic fork, event handlers, task domains, JSON workflows, worker polling, sub-workflows). Tokenizes the query by lowercasing, stripping non-alphanumeric characters via `replaceAll("[^a-z0-9 ]", " ")`, filtering tokens shorter than 2 characters, and computing Jaccard similarity (intersection/union of token sets). Returns the top 3 documents sorted by descending relevance, rounded to 2 decimal places. Off-topic queries like pricing naturally score below 0.3.

**GradeRelevanceWorker** (`cr_grade_relevance`) -- Iterates the documents list, summing each entry's `relevance` field (cast from `Number` to `double`). Computes `avg = sum / count` and returns `verdict: "relevant"` if avg >= 0.5, `"irrelevant"` otherwise. Formats the average to 2 decimal places via `String.format("%.2f", avg)`.

**GenerateAnswerWorker** (`cr_generate_answer`) -- The "relevant" branch. Calls OpenAI Chat Completions (`gpt-4o-mini`, `max_tokens: 512`, `temperature: 0.3`) with a system prompt instructing concise context-based answers. Requires `CONDUCTOR_OPENAI_API_KEY`. Distinguishes retryable errors (429, 503 -> `FAILED`) from terminal errors (other 4xx -> `FAILED_WITH_TERMINAL_ERROR`).

**WebSearchWorker** (`cr_web_search`) -- The "irrelevant" fallback. Queries Wikipedia's search API at `en.wikipedia.org/w/api.php` with `srlimit=3`. Parses the JSON response using regex patterns (`"title"\s*:\s*"([^"]+)"` and `"snippet"\s*:\s*"([^"]+)"`), strips HTML tags via `replaceAll("<[^>]+>", "")`, and decodes entities (`&quot;`, `&amp;`, `&lt;`, `&gt;`). Uses a 10-second connect timeout and `User-Agent: ConductorExample/1.0`.

**GenerateFromWebWorker** (`cr_generate_from_web`) -- Generates from web results using the same OpenAI setup as GenerateAnswerWorker. Estimates token usage as `answer.split("\\s+").length * 2`.

## Tests

20 tests across 5 test files cover Jaccard retrieval scoring, relevance grading thresholds, web search parsing, and both generation paths.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
