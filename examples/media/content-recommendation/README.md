# Content Recommendation Engine in Java Using Conductor : History Analysis, Similarity Scoring, Ranking, Filtering, and Serving

## Why Recommendation Pipelines Need Orchestration

Building a recommendation involves a multi-stage pipeline where each stage refines the previous one's output. You analyze the user's history. viewed items, liked items, top categories, activity score. You compute similarity scores between the user's preferences and the content catalog, generating candidate items with relevance scores. You rank those candidates using a learned ranking model. You apply business filters, removing already-viewed content, enforcing category diversity, respecting content freshness rules. Finally, you serve the filtered, ranked list to the user with sub-200ms response time.

Each stage depends on the previous one. similarity scoring needs user history, ranking needs similarity scores, filtering needs ranked results. If the history lookup fails, you cannot compute relevant similarities. If you skip filtering, users see content they have already viewed. Without orchestration, you'd build a monolithic recommendation engine that mixes user profiling, embedding lookups, ML inference, and business logic, making it impossible to swap your ranking model, test filter rules independently, or measure which stage is the latency bottleneck.

## How This Workflow Solves It

**You just write the recommendation workers. History analysis, similarity scoring, ranking, filtering, and serving. Conductor handles stage sequencing, model endpoint retries, and response-time tracking for latency optimization.**

Each recommendation stage is an independent worker. analyze history, compute similarity, rank results, apply filters, serve recommendations. Conductor sequences them, passes user profiles and candidate lists between stages, retries if a model endpoint times out, and tracks response time and cache hit rates for performance optimization.

### What You Write: Workers

Five workers build the recommendation pipeline: AnalyzeHistoryWorker profiles user preferences, ComputeSimilarityWorker scores content candidates, RankResultsWorker orders by relevance, ApplyFiltersWorker enforces diversity rules, and ServeRecommendationsWorker delivers personalized results.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeHistoryWorker** | `crm_analyze_history` | Analyzes the history |
| **ApplyFiltersWorker** | `crm_apply_filters` | Applies filters |
| **ComputeSimilarityWorker** | `crm_compute_similarity` | Computes the similarity |
| **RankResultsWorker** | `crm_rank_results` | Ranks results |
| **ServeRecommendationsWorker** | `crm_serve_recommendations` | Handles serve recommendations |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
crm_analyze_history
 │
 ▼
crm_compute_similarity
 │
 ▼
crm_rank_results
 │
 ▼
crm_apply_filters
 │
 ▼
crm_serve_recommendations

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
