# Seo Workflow

Orchestrates seo workflow through a multi-stage Conductor workflow.

**Input:** `siteUrl`, `targetKeywords`, `pageUrl` | **Timeout:** 60s

## Pipeline

```
seo_audit_site
    │
seo_research_keywords
    │
seo_optimize_content
    │
seo_submit_sitemap
    │
seo_monitor_rankings
```

## Workers

**AuditSiteWorker** (`seo_audit_site`)

Reads `seoScore`. Outputs `seoScore`, `issues`, `currentRankings`.

**MonitorRankingsWorker** (`seo_monitor_rankings`)

Reads `monitoringStatus`. Outputs `monitoringStatus`, `trackedKeywordCount`, `checkFrequency`, `alertThreshold`.

**OptimizeContentWorker** (`seo_optimize_content`)

Reads `optimizedPages`. Outputs `optimizedPages`, `metaDescriptionsAdded`, `headingsOptimized`, `internalLinksAdded`.

**ResearchKeywordsWorker** (`seo_research_keywords`)

Reads `topKeywords`. Outputs `topKeywords`, `keyword`, `volume`, `difficulty`, `currentRank`.

**SubmitSitemapWorker** (`seo_submit_sitemap`)

Reads `submissionId`. Outputs `submissionId`, `submittedTo`, `pagesInSitemap`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
