# SEO Optimization Pipeline in Java Using Conductor :  Site Audit, Keyword Research, Content Optimization, Sitemap Submission, and Rank Monitoring

A Java Conductor workflow example that orchestrates an SEO optimization pipeline. auditing site health with SEO scoring and issue detection, researching target keywords with search volume and difficulty metrics, optimizing page content (meta descriptions, heading structure, internal linking), submitting updated sitemaps to search engines, and setting up ongoing rank monitoring with alert thresholds. Uses [Conductor](https://github.

## Why SEO Workflows Need Orchestration

Improving search rankings requires a pipeline where each step informs the next. You audit the site to identify SEO issues. missing meta descriptions, poor heading structure, broken internal links, and compute an overall SEO score with current rankings. You research keywords to find opportunities,  identifying terms with high search volume but achievable difficulty where the site does not currently rank. You optimize content pages based on the audit findings and keyword research,  adding meta descriptions, restructuring headings, inserting internal links. You submit the updated sitemap to search engines so changes are crawled promptly. Finally, you set up rank monitoring with alert thresholds to track whether the optimizations are improving positions.

Each stage depends on the previous one. keyword research needs the audit's current rankings, optimization needs the keyword targets, and sitemap submission needs the optimized pages. Without orchestration, you'd build a monolithic SEO tool that mixes crawling, keyword APIs, content modification, and rank tracking,  making it impossible to swap your keyword research provider, test content optimization rules independently, or trace which optimization caused a ranking change.

## How This Workflow Solves It

**You just write the SEO workers. Site audit, keyword research, content optimization, sitemap submission, and rank monitoring. Conductor handles audit-to-monitoring sequencing, search engine API retries, and before/after records for measuring optimization impact.**

Each SEO stage is an independent worker. audit site, research keywords, optimize content, submit sitemap, monitor rankings. Conductor sequences them, passes SEO scores and keyword targets between stages, retries if a search engine API times out, and records every optimization action for before/after comparison.

### What You Write: Workers

Five workers cover the SEO cycle: AuditSiteWorker identifies technical issues, ResearchKeywordsWorker finds ranking opportunities, OptimizeContentWorker improves on-page signals, SubmitSitemapWorker notifies search engines, and MonitorRankingsWorker tracks position changes.

| Worker | Task | What It Does |
|---|---|---|
| **AuditSiteWorker** | `seo_audit_site` | Audits the site |
| **MonitorRankingsWorker** | `seo_monitor_rankings` | Monitors rankings |
| **OptimizeContentWorker** | `seo_optimize_content` | Optimizes the content |
| **ResearchKeywordsWorker** | `seo_research_keywords` | Handles research keywords |
| **SubmitSitemapWorker** | `seo_submit_sitemap` | Handles submit sitemap |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
seo_audit_site
    │
    ▼
seo_research_keywords
    │
    ▼
seo_optimize_content
    │
    ▼
seo_submit_sitemap
    │
    ▼
seo_monitor_rankings

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/seo-workflow-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/seo-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow seo_workflow \
  --version 1 \
  --input '{"siteUrl": "https://example.com", "targetKeywords": "production", "pageUrl": "https://example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w seo_workflow -s COMPLETED -c 5

```

## How to Extend

Connect AuditSiteWorker to your crawler (Screaming Frog, Sitebulb), ResearchKeywordsWorker to a keyword API (Ahrefs, SEMrush), and MonitorRankingsWorker to your rank tracking tool. The workflow definition stays exactly the same.

- **AuditSiteWorker** (`seo_audit_site`): run a real site audit using Screaming Frog, Ahrefs Site Audit API, or a custom crawler to detect missing meta tags, broken links, and compute SEO scores
- **ResearchKeywordsWorker** (`seo_research_keywords`): query keyword research APIs (Ahrefs, SEMrush, Google Keyword Planner) for search volume, keyword difficulty, and current ranking positions
- **OptimizeContentWorker** (`seo_optimize_content`): update pages in your CMS with optimized meta descriptions, restructured headings, and internal links based on keyword targets
- **SubmitSitemapWorker** (`seo_submit_sitemap`): submit the updated sitemap to Google Search Console and Bing Webmaster Tools via their APIs
- **MonitorRankingsWorker** (`seo_monitor_rankings`): set up rank tracking via your SEO tool's API (Ahrefs, SEMrush, Moz) with keyword-level monitoring and position change alerts

Wire each worker to your crawling tools or rank tracking service while keeping the same return fields, and the optimization pipeline needs no changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
seo-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/seoworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SeoWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditSiteWorker.java
│       ├── MonitorRankingsWorker.java
│       ├── OptimizeContentWorker.java
│       ├── ResearchKeywordsWorker.java
│       └── SubmitSitemapWorker.java
└── src/test/java/seoworkflow/workers/
    ├── AuditSiteWorkerTest.java        # 2 tests
    └── ResearchKeywordsWorkerTest.java        # 2 tests

```
