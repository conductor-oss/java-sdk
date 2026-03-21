# Podcast Production Pipeline in Java Using Conductor :  Recording, Audio Editing, Transcription, Publishing, and Directory Distribution

A Java Conductor workflow example that orchestrates podcast production. ingesting raw audio recordings with sample rate and channel metadata, editing (silence removal, normalization, compression, bitrate optimization), transcribing speech to text with language detection and word-level confidence, publishing the episode with RSS feed generation, and distributing to podcast directories (Apple Podcasts, Spotify, Google Podcasts). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Podcast Production Needs Orchestration

Producing a podcast episode involves a multi-stage pipeline where each step transforms or enriches the audio. You ingest the raw recording. capturing duration, sample rate, and channel count. You edit the audio,  removing silence, normalizing levels, applying compression, and encoding at the target bitrate to reduce file size. You transcribe the edited audio for show notes, accessibility, and SEO. You publish the episode,  generating the RSS feed entry with metadata, duration, and file URLs. Finally, you ping all major podcast directories so the new episode appears in listeners' feeds.

Each stage depends on the previous one. you edit the raw recording (not a published one), you transcribe the edited version (not the raw), and you publish with accurate duration from the edited file. If transcription fails, you still want to publish; but the transcript should be retried independently. Without orchestration, you'd build a monolithic podcast pipeline that mixes FFmpeg commands, speech-to-text API calls, RSS generation, and directory API pings,  making it impossible to swap your transcription provider, test editing settings independently, or trace which processing step introduced an audio artifact.

## How This Workflow Solves It

**You just write the podcast production workers. Recording ingestion, audio editing, transcription, publishing, and directory distribution. Conductor handles stage ordering, transcription API retries, and a complete production history for every episode.**

Each production stage is an independent worker. record, edit, transcribe, publish, distribute. Conductor sequences them, passes audio URLs and metadata between stages, retries if a transcription API times out, and keeps a complete production history for every episode.

### What You Write: Workers

Five workers handle podcast production: RecordWorker ingests raw audio with metadata, EditWorker processes audio levels and bitrate, TranscribeWorker converts speech to text, PublishWorker generates the RSS feed entry, and DistributeWorker pings podcast directories.

| Worker | Task | What It Does |
|---|---|---|
| **DistributeWorker** | `pod_distribute` | Distributes the content and computes directories, pings sent, distributed at |
| **EditWorker** | `pod_edit` | Edits the content and computes edited audio url, final duration, file size mb, bitrate |
| **PublishWorker** | `pod_publish` | Publishes the content and computes episode url, rss url, published at |
| **RecordWorker** | `pod_record` | Records the input and computes raw audio url, duration minutes, sample rate, channels |
| **TranscribeWorker** | `pod_transcribe` | Transcribes the podcast audio into text. Produces an SRT transcript URL, word count, detected language, and confidence score |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
pod_record
    │
    ▼
pod_edit
    │
    ▼
pod_transcribe
    │
    ▼
pod_publish
    │
    ▼
pod_distribute

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
java -jar target/podcast-workflow-1.0.0.jar

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
java -jar target/podcast-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow podcast_workflow \
  --version 1 \
  --input '{"episodeId": "TEST-001", "showName": "test", "episodeTitle": "sample-episodeTitle", "hostId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w podcast_workflow -s COMPLETED -c 5

```

## How to Extend

Connect EditWorker to your audio processing pipeline (FFmpeg, Auphonic), TranscribeWorker to a speech-to-text service (Whisper, Deepgram), and DistributeWorker to podcast directory APIs (Apple, Spotify). The workflow definition stays exactly the same.

- **RecordWorker** (`pod_record`): ingest audio from your recording platform (Riverside, Zencastr, Descript) or upload raw files, extracting duration, sample rate, and channel metadata
- **EditWorker** (`pod_edit`): process audio using FFmpeg or a cloud audio API (Descript, Auphonic) for silence removal, loudness normalization, dynamic compression, and bitrate encoding
- **TranscribeWorker** (`pod_transcribe`): transcribe the edited audio using a speech-to-text service (Whisper, Deepgram, AssemblyAI) with language detection and word-level confidence scores
- **PublishWorker** (`pod_publish`): publish the episode to your podcast hosting platform (Buzzsprout, Transistor, Anchor) and generate the RSS feed entry with metadata
- **DistributeWorker** (`pod_distribute`): ping podcast directories (Apple Podcasts Connect, Spotify for Podcasters, Google Podcasts) to announce the new episode and trigger feed refresh

Plug each worker into your audio processing tools or transcription API while preserving output fields, and the production pipeline requires no changes.

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
podcast-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/podcastworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PodcastWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DistributeWorker.java
│       ├── EditWorker.java
│       ├── PublishWorker.java
│       ├── RecordWorker.java
│       └── TranscribeWorker.java
└── src/test/java/podcastworkflow/workers/
    ├── EditWorkerTest.java        # 2 tests
    └── RecordWorkerTest.java        # 2 tests

```
