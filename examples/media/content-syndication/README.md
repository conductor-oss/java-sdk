# Content Syndication Pipeline in Java Using Conductor :  Content Selection, Platform Formatting, Multi-Channel Distribution, and Performance Tracking

A Java Conductor workflow example that orchestrates content syndication .  selecting content from your CMS with metadata and word counts, reformatting for each target platform's requirements (Medium, Dev.to, Hashnode character limits and markup), distributing to all platforms simultaneously, and setting up UTM-tagged tracking pixels for cross-platform performance measurement. Uses [Conductor](https://github.

## Why Content Syndication Needs Orchestration

Syndicating content to multiple platforms requires adapting the same source content for each destination's format constraints. You select the content and extract its body, category, and word count. You reformat it for each platform. Medium requires specific HTML, Dev.to uses front matter with liquid tags, Hashnode has its own markdown flavor, each with different character limits. You distribute to all platforms and collect the published URLs. You set up tracking with UTM campaign parameters and pixel tags to measure which syndication channels drive the most traffic back to your site.

If formatting fails for one platform, you still want to syndicate to the others. If distribution succeeds but tracking setup fails, you need to resume at tracking .  not re-publish and create duplicates. Without orchestration, you'd build a monolithic syndication script with platform-specific formatting logic, API clients for each platform, and tracking code all tangled together ,  making it impossible to add a new platform without modifying the core publishing logic.

## How This Workflow Solves It

**You just write the syndication workers. Content selection, platform formatting, multi-channel distribution, and performance tracking. Conductor handles per-platform retries, parallel distribution, and records linking published URLs to traffic metrics.**

Each syndication stage is an independent worker .  select content, format per platform, distribute, track performance. Conductor sequences them, passes content bodies and platform-specific versions between stages, retries if a platform API is temporarily down, and records which content was published where and when.

### What You Write: Workers

Four workers handle cross-platform syndication: SelectContentWorker picks articles from the CMS, FormatPerPlatformWorker adapts markup for each destination, DistributeWorker publishes to all platforms, and TrackPerformanceWorker sets up UTM-tagged tracking.

| Worker | Task | What It Does |
|---|---|---|
| **DistributeWorker** | `syn_distribute` | Distributes the content and computes distributed platforms, distributed count, urls, medium |
| **FormatPerPlatformWorker** | `syn_format_per_platform` | Formats the per platform |
| **SelectContentWorker** | `syn_select_content` | Handles select content |
| **TrackPerformanceWorker** | `syn_track_performance` | Tracks the performance |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

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
java -jar target/content-syndication-1.0.0.jar

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
java -jar target/content-syndication-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_syndication_workflow \
  --version 1 \
  --input '{"contentId": "TEST-001", "title": "sample-title", "platforms": "sample-platforms", "publishDate": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_syndication_workflow -s COMPLETED -c 5

```

## How to Extend

Connect FormatPerPlatformWorker to each platform's API (Medium, Dev.to, Hashnode), DistributeWorker to their publishing endpoints, and TrackPerformanceWorker to your UTM analytics. The workflow definition stays exactly the same.

- **SelectContentWorker** (`syn_select_content`): query your CMS API (WordPress, Ghost, Contentful) to select content ready for syndication and extract body, metadata, category, and word count
- **FormatPerPlatformWorker** (`syn_format_per_platform`): transform content for each platform's format: Medium HTML, Dev.to markdown with front matter, Hashnode markdown, adjusting character counts and markup
- **DistributeWorker** (`syn_distribute`): publish to each platform via their APIs (Medium API, Dev.to API, Hashnode GraphQL) and collect published URLs for each
- **TrackPerformanceWorker** (`syn_track_performance`): set up UTM-tagged canonical URLs, tracking pixels, and cross-platform analytics to measure referral traffic from each syndication channel

Wire each worker to a real platform API while keeping the same return fields, and the syndication pipeline adapts without modification.

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
content-syndication/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentsyndication/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentSyndicationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DistributeWorker.java
│       ├── FormatPerPlatformWorker.java
│       ├── SelectContentWorker.java
│       └── TrackPerformanceWorker.java
└── src/test/java/contentsyndication/workers/
    ├── FormatPerPlatformWorkerTest.java        # 2 tests
    └── SelectContentWorkerTest.java        # 2 tests

```
