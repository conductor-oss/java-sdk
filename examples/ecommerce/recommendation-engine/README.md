# Recommendation Engine in Java Using Conductor : Collect Behavior, Compute Similarity, Rank, Personalize

## Generic "Popular Items" Lists Don't Convert

Showing every customer the same "Top Sellers" list misses the point of personalization. A customer who buys hiking gear should see trail shoes and backpacks, not kitchen appliances. Collaborative filtering finds patterns: "Customers who bought X also bought Y." Content-based filtering uses item attributes: "This hiking boot is similar to the one you viewed." A good recommendation engine combines both.

The pipeline collects behavioral signals (what did this user view, purchase, and wishlist?), computes similarity scores between items (based on co-purchase patterns and item attributes), ranks candidates by predicted relevance for this specific user, and personalizes the final list (boosting items matching the user's current session context). Each step narrows the focus from millions of products to a ranked list of 10-20 recommendations.

## The Solution

**You just write the behavior collection, similarity computation, candidate ranking, and personalization logic. Conductor handles scoring retries, candidate pipeline sequencing, and recommendation audit trails.**

`CollectBehaviorWorker` gathers the user's recent activity. page views, purchases, search queries, wishlist additions, and cart interactions, as behavioral signals. `ComputeSimilarityWorker` calculates item-to-item similarity using collaborative filtering (co-purchase patterns) and content-based features (category, price range, attributes). `RankCandidatesWorker` scores candidate products by predicted relevance for this user, combining collaborative and content-based signals. `PersonalizeWorker` adjusts the final ranking based on context, boosting items matching the current session, applying diversity rules (don't show 5 similar items), and filtering already-purchased items. Conductor records which recommendations were generated and, combined with click/purchase tracking, enables recommendation quality measurement.

### What You Write: Workers

Profile analysis, candidate generation, scoring, and delivery workers each own one layer of the recommendation pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **CollectBehaviorWorker** | `rec_collect_behavior` | Gathers user browsing behavior: viewed products, purchased products, browsing categories, and session count. |
| **ComputeSimilarityWorker** | `rec_compute_similarity` | Computes similarity scores between user behavior and candidate products. |
| **PersonalizeWorker** | `rec_personalize` | Personalizes the top-ranked products for the user's context, returning the top 3 with a reason string. |
| **RankCandidatesWorker** | `rec_rank_candidates` | Ranks candidate products by similarity score and assigns a rank number. |

### The Workflow

```
rec_collect_behavior
 │
 ▼
rec_compute_similarity
 │
 ▼
rec_rank_candidates
 │
 ▼
rec_personalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
