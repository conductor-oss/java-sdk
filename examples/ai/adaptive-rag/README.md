# Adaptive RAG: Routing Queries to Different Retrieval Strategies Based on LLM Classification

Not all questions are equal. "What is Conductor?" needs a quick document lookup. "Compare Conductor and Temporal's architecture trade-offs" requires gathering evidence from multiple sources, building a reasoning chain, then synthesizing. "Write a poem about microservices" needs no retrieval at all. A one-size-fits-all RAG pipeline either over-fetches for simple queries (wasting tokens and latency) or under-fetches for complex ones (producing shallow answers).

This example implements a query router that classifies questions via GPT-4o-mini, then uses Conductor's SWITCH task to dispatch to three completely different retrieval-and-generation pipelines -- each with its own document corpus, retrieval algorithm, and generation prompt.

## The Three Paths

```
ar_classify  (GPT-4o-mini -> {queryType: factual|analytical|creative, confidence: 0.XX})
     |
     v
SWITCH on queryType:
     |
     |── "factual"
     |      ar_simple_ret  (Jaccard similarity, 8-doc corpus, top 2)
     |      ar_simple_gen  (concise factual answer, temp 0.3)
     |
     |── "analytical"
     |      ar_mhop_ret    (2-hop retrieval: 4 factual + 4 analytical docs, query expansion)
     |      ar_reason      (chain-of-thought: 3 reasoning steps from multi-hop docs)
     |      ar_anal_gen    (synthesize from reasoning chain, temp 0.3)
     |
     |── default (creative)
            ar_creative_gen (free-form generation, no retrieval, temp 0.9)
```

## Worker: ClassifyWorker (`ar_classify`) -- The Query Router

Sends the question to GPT-4o-mini with `temperature: 0.1` and a structured prompt requesting `{"queryType": "...", "confidence": 0.XX}`. The classification criteria are documented in the source:

- **factual** -- simple lookups, definitions, "what is", "who is", "when did"
- **analytical** -- comparisons ("compare", "vs", "trade-offs"), multi-step reasoning ("how does X affect Y"), cause/effect, pros/cons
- **creative** -- imaginative ("write a poem", "tell a story", "imagine"), brainstorming, hypothetical scenarios

The returned `queryType` is validated against `List.of("factual", "analytical", "creative")`. If the LLM returns anything else (e.g., `"opinion"` or `"technical"`), the worker fails with a clear message listing the valid types.

## Factual Path: Single-Hop Retrieval

### SimpleRetrieveWorker (`ar_simple_ret`)

Uses Jaccard similarity (set intersection over set union) against an 8-document knowledge base about Conductor. Tokenization lowercases text, strips non-alphanumeric characters, and filters tokens under 2 characters. Returns exactly 2 documents, always sorted by similarity score.

```java
Set<String> intersection = new HashSet<>(a);
intersection.retainAll(b);
Set<String> union = new HashSet<>(a);
union.addAll(b);
return (double) intersection.size() / union.size();
```

### SimpleGenerateWorker (`ar_simple_gen`)

Feeds the 2 retrieved documents as context to GPT-4o-mini with system prompt "Be concise and factual." Tags output with `strategy: "simple_rag"`.

## Analytical Path: Multi-Hop Retrieval with Chain-of-Thought

### MultiHopRetrieveWorker (`ar_mhop_ret`)

Maintains two separate document corpora:

**Hop 1 (4 docs):** Direct factual content about Conductor and Temporal -- their architectures, SDK support, UI features.

**Hop 2 (4 docs):** Deeper analytical content -- orchestration vs choreography trade-offs, centralization pros/cons, Temporal's replay architecture.

The algorithm works in two stages. First, it finds the 2 most relevant Hop 1 documents using Jaccard similarity against the original query. Then it expands the query tokens by adding all tokens from the Hop 1 results:

```java
Set<String> expandedTokens = new HashSet<>(queryTokens);
for (Map<String, Object> doc : hop1Results) {
    expandedTokens.addAll(tokenize((String) doc.get("text")));
}
```

This expanded token set is used to search Hop 2, pulling in analytical documents related to concepts discovered in Hop 1. Each returned document carries a `hop` metadata field (1 or 2) for traceability.

### ReasoningWorker (`ar_reason`)

Takes the 4 multi-hop documents and asks GPT-4o-mini to produce exactly 3 chain-of-thought reasoning steps, each building on the previous. The output is parsed by splitting on newlines and filtering blank lines.

### AnalyticalGenerateWorker (`ar_anal_gen`)

Synthesizes from the reasoning chain (not the raw documents) using system prompt "Compare and contrast different aspects." Tags output with `strategy: "multi_hop_rag"`.

## Creative Path: No Retrieval

### CreativeGenerateWorker (`ar_creative_gen`)

Bypasses retrieval entirely. Sends the question directly to GPT-4o-mini with `temperature: 0.9` for maximum variety. Accepts an optional `style` parameter (defaults to `"creative"`). Tags output with `strategy: "creative"`.

## Error Handling Across All Workers

Every OpenAI-calling worker uses the same pattern: HTTP 429 and 503 are `FAILED` (retryable by Conductor), all other HTTP errors are `FAILED_WITH_TERMINAL_ERROR`. Every failure includes `errorBody` and `httpStatus` in the output for debugging. The ClassifyWorker constructor throws `IllegalStateException` if `CONDUCTOR_OPENAI_API_KEY` is not set, failing fast at worker registration time rather than at task execution time.

## Test Coverage

5 test classes, 11 tests:

**AdaptiveRagIntegrationTest (4 tests):** Factual path produces 2 documents compatible with SimpleGenerateWorker's `List<String>` input contract. Analytical path produces 4 documents with hop metadata compatible with ReasoningWorker's `List<Map>` input. Same query on both paths returns non-overlapping document sets. Blank question rejection across all retrieval paths.

**SimpleRetrieveWorkerTest, MultiHopRetrieveWorkerTest, ClassifyWorkerTest, and other worker tests:** Task def names, API key requirements, blank input rejection.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `CONDUCTOR_OPENAI_API_KEY` | (required) | OpenAI API authentication for classify, generate, and reason workers |
| All LLM workers | `gpt-4o-mini` | Model used for classification, reasoning, and generation |

---

## Production Notes

See [PRODUCTION.md](PRODUCTION.md) for deployment guidance, monitoring expectations, and security considerations.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
