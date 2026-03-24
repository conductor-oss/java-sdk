# Customer Segmentation in Java Using Conductor : Collect Data, Cluster, Label Segments, Target

Customer segmentation: collect data, cluster, label segments, target.

## One-Size-Fits-All Marketing Wastes Budget

Sending the same promotion to all customers means high-value VIPs get the same generic email as price-sensitive bargain hunters. Customer segmentation groups customers by behavior. frequency of purchase, average order value, product preferences, engagement level, so each group gets tailored messaging.

The segmentation pipeline collects raw behavioral data, applies clustering algorithms (k-means, RFM analysis) to identify natural groupings, labels each cluster with human-readable descriptions ("High-value loyalists", "Price-sensitive browsers", "At-risk churners"), and generates segment-specific marketing strategies. If the clustering step reveals an unexpected segment, the labeling and targeting steps need to adapt. Each run produces different segments as customer behavior evolves.

## The Solution

**You just write the data collection, clustering, segment labeling, and campaign targeting logic. Conductor handles clustering retries, pipeline ordering, and segment tracking across campaign runs.**

`CollectDataWorker` gathers customer behavioral data. purchase frequency, average order value, recency of last purchase, browsing patterns, and engagement metrics, for the specified dataset. `ClusterWorker` applies clustering algorithms to group customers by behavioral similarity, producing segment assignments with cluster centroids. `LabelSegmentsWorker` analyzes each cluster's characteristics and assigns descriptive labels, "VIP loyalists", "New high-potential", "Dormant at-risk", "Price-sensitive active". `TargetWorker` generates segment-specific marketing strategies, retention offers for at-risk segments, upsell campaigns for high-potential segments, loyalty rewards for VIPs. Conductor records each segmentation run for trend analysis across runs.

### What You Write: Workers

Data collection, clustering, segment labeling, and targeting workers form a pipeline where each step refines customer groups without coupling to the others.

| Worker | Task | What It Does |
|---|---|---|
| **ClusterWorker** | `seg_cluster` | Running k-means with k= |
| **CollectDataWorker** | `seg_collect_data` | Loading dataset |
| **LabelSegmentsWorker** | `seg_label_segments` | Labeling segments based on centroid characteristics |
| **TargetWorker** | `seg_target` | Performs the target operation |

### The Workflow

```
seg_collect_data
 │
 ▼
seg_cluster
 │
 ▼
seg_label_segments
 │
 ▼
seg_target

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
