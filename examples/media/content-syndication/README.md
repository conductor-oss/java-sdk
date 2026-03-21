# Content Syndication Pipeline in Java Using Conductor : Content Selection, Platform Formatting, Multi-Channel Distribution, and Performance Tracking

## Why Content Syndication Needs Orchestration

Syndicating content to multiple platforms requires adapting the same source content for each destination's format constraints. You select the content and extract its body, category, and word count. You reformat it for each platform. Medium requires specific HTML, Dev.to uses front matter with liquid tags, Hashnode has its own markdown flavor, each with different character limits. You distribute to all platforms and collect the published URLs. You set up tracking with UTM campaign parameters and pixel tags to measure which syndication channels drive the most traffic back to your site.

If formatting fails for one platform, you still want to syndicate to the others. If distribution succeeds but tracking setup fails, you need to resume at tracking. not re-publish and create duplicates. Without orchestration, you'd build a monolithic syndication script with platform-specific formatting logic, API clients for each platform, and tracking code all tangled together, making it impossible to add a new platform without modifying the core publishing logic.

## How This Workflow Solves It

**You just write the syndication workers. Content selection, platform formatting, multi-channel distribution, and performance tracking. Conductor handles per-platform retries, parallel distribution, and records linking published URLs to traffic metrics.**

Each syndication stage is an independent worker. select content, format per platform, distribute, track performance. Conductor sequences them, passes content bodies and platform-specific versions between stages, retries if a platform API is temporarily down, and records which content was published where and when.

### What You Write: Workers

Four workers handle cross-platform syndication: SelectContentWorker picks articles from the CMS, FormatPerPlatformWorker adapts markup for each destination, DistributeWorker publishes to all platforms, and TrackPerformanceWorker sets up UTM-tagged tracking.

| Worker | Task | What It Does |
|---|---|---|
| **DistributeWorker** | `syn_distribute` | Distributes the content and computes distributed platforms, distributed count, urls, medium |
| **FormatPerPlatformWorker** | `syn_format_per_platform` | Formats the per platform |
| **SelectContentWorker** | `syn_select_content` | Handles select content |
| **TrackPerformanceWorker** | `syn_track_performance` | Tracks the performance |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
syn_select_content
 │
 ▼
syn_format_per_platform
 │
 ▼
syn_distribute
 │
 ▼
syn_track_performance

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
