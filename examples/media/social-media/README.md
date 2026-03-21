# Social Media Management in Java Using Conductor :  Content Creation, Scheduling, Publishing, Engagement Monitoring, and Community Response

A Java Conductor workflow example that orchestrates social media management. creating formatted posts with hashtag optimization and optimal posting time selection, scheduling for peak engagement windows, publishing to social platforms, monitoring engagement metrics (impressions, likes, shares, comments, mentions, engagement rate), and handling community responses (replies, likes, flagging for review). Uses [Conductor](https://github.

## Why Social Media Management Needs Orchestration

Managing social media presence involves a lifecycle for every post. You create the content. formatting the message, selecting relevant hashtags, and determining the optimal posting time based on audience analytics. You schedule the post for the peak engagement window. You publish to the platform and capture the post URL and ID. You monitor engagement over time,  tracking impressions, likes, shares, comments, mentions, and computing the overall engagement rate. You respond to community interactions,  replying to comments, liking fan responses, and flagging negative interactions for review.

Each stage depends on the previous one. you cannot schedule before creating, cannot monitor before publishing, and cannot respond without engagement data. If publishing fails due to a rate limit, you need to retry at the next available window,  not re-create the content. Without orchestration, you'd build a monolithic social media tool that mixes content creation, platform API calls, analytics polling, and community management,  making it impossible to add a new platform, test scheduling algorithms independently, or audit which posts generated the most engagement.

## How This Workflow Solves It

**You just write the social media workers. Content creation, scheduling, publishing, engagement monitoring, and community response. Conductor handles publish-before-monitor sequencing, rate-limit retries, and a complete post history from creation through engagement outcomes.**

Each social media stage is an independent worker. create content, schedule, publish, monitor engagement, engage responses. Conductor sequences them, passes post IDs and scheduled times between stages, retries if a platform API rate-limits, and provides a complete history of every post from creation through engagement outcomes.

### What You Write: Workers

Five workers manage the social lifecycle: CreateContentWorker formats posts with hashtags, SchedulePostWorker picks peak engagement windows, PublishPostWorker pushes to the platform, MonitorEngagementWorker tracks impressions and interactions, and EngageResponsesWorker handles community replies.

| Worker | Task | What It Does |
|---|---|---|
| **CreateContentWorker** | `soc_create_content` | Creates the content |
| **EngageResponsesWorker** | `soc_engage_responses` | Handles engage responses |
| **MonitorEngagementWorker** | `soc_monitor_engagement` | Monitors the engagement |
| **PublishPostWorker** | `soc_publish_post` | Publishes the post |
| **SchedulePostWorker** | `soc_schedule_post` | Schedules the post |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
soc_create_content
    │
    ▼
soc_schedule_post
    │
    ▼
soc_publish_post
    │
    ▼
soc_monitor_engagement
    │
    ▼
soc_engage_responses

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
java -jar target/social-media-1.0.0.jar

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
java -jar target/social-media-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow social_media_workflow \
  --version 1 \
  --input '{"campaignId": "TEST-001", "platform": "sample-platform", "message": "Process this order for customer C-100", "mediaUrl": "https://example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w social_media_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CreateContentWorker to your content authoring tool, PublishPostWorker to the platform APIs (Twitter/X, LinkedIn, Instagram), and MonitorEngagementWorker to your social analytics dashboard. The workflow definition stays exactly the same.

- **CreateContentWorker** (`soc_create_content`): generate post content using your content calendar, format with platform-specific constraints (character limits, image specs), and select hashtags and optimal posting times from your analytics
- **SchedulePostWorker** (`soc_schedule_post`): schedule the post via your social media management tool (Buffer, Hootsuite, Sprout Social) for the peak engagement window
- **PublishPostWorker** (`soc_publish_post`): publish to social platforms via their APIs (Twitter/X, LinkedIn, Instagram Graph API, Facebook) and capture post URLs
- **MonitorEngagementWorker** (`soc_monitor_engagement`): poll platform analytics APIs for impressions, likes, shares, comments, and mentions over the monitoring window
- **EngageResponsesWorker** (`soc_engage_responses`): auto-reply to comments, like fan responses, and flag negative interactions for human review using your community management rules

Connect each worker to your social platform API while preserving output fields, and the publishing flow stays the same.

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
social-media/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/socialmedia/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SocialMediaExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateContentWorker.java
│       ├── EngageResponsesWorker.java
│       ├── MonitorEngagementWorker.java
│       ├── PublishPostWorker.java
│       └── SchedulePostWorker.java
└── src/test/java/socialmedia/workers/
    ├── CreateContentWorkerTest.java        # 2 tests
    └── SchedulePostWorkerTest.java        # 2 tests

```
