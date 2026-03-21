# AI Music Generation in Java Using Conductor :  Compose, Arrange, Produce, Master, Deliver

A Java Conductor workflow that generates music through a five-stage production pipeline. composing a melody and chord progression for the specified genre and mood, arranging with instrumentation, producing with effects and mixing, mastering for final polish, and delivering the audio file. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the music production pipeline as independent workers,  you write the music generation logic, Conductor handles sequencing, retries, durability, and observability.

## Music Production Has Distinct Creative Stages

Generating a 30-second lofi hip-hop track in a relaxed mood requires five professional production stages: composition (melody, chord progression, rhythm pattern in the right key and tempo), arrangement (selecting instruments. piano, bass, drums, synth pads, and assigning parts), production (applying effects,  reverb, compression, EQ, and mixing levels), mastering (final loudness normalization, stereo width, format compliance), and delivery (encoding to the target format with metadata).

Each stage transforms the music: composition creates the musical ideas, arrangement gives them sonic identity, production shapes the sound, and mastering ensures it sounds good on any playback system. If the arrangement step produces an instrument combination that sounds muddy, you can retry it without recomposing the melody.

## The Solution

**You just write the composition, arrangement, production, mastering, and delivery logic. Conductor handles retry logic, stage ordering, and production audit trails from composition to final master.**

`ComposeWorker` generates the core musical elements. melody, chord progression, rhythm, key, tempo, and structure,  matching the requested genre and mood. `ArrangeWorker` selects instruments and assigns parts to create the full arrangement. `ProduceWorker` applies effects (reverb, compression, EQ), mixes levels, and adds production polish. `MasterWorker` normalizes loudness, adjusts stereo width, and ensures format compliance. `DeliverWorker` encodes the final audio with metadata (genre, BPM, key, duration) and delivers the file. Conductor records the full production chain for creative iteration.

### What You Write: Workers

From composition through mastering, each worker in this pipeline owns a single phase of AI music production and passes structured output to the next.

| Worker | Task | What It Does |
|---|---|---|
| **ArrangeWorker** | `amg_arrange` | Arranges the composition for piano, strings, and drums. selects instruments and assigns parts for the target duration |
| **ComposeWorker** | `amg_compose` | Composes the core musical elements. melody, chord progression, rhythm, key, and tempo matching the requested genre and mood |
| **DeliverWorker** | `amg_deliver` | Delivers the mastered track as WAV format to the output destination with file size metadata |
| **MasterWorker** | `amg_master` | Masters the produced track. normalizes loudness to -14 LUFS and applies final polish for distribution |
| **ProduceWorker** | `amg_produce` | Produces the track. applies mixing, effects (reverb, compression, EQ), and balances audio levels at 48kHz/24-bit |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
amg_compose
    │
    ▼
amg_arrange
    │
    ▼
amg_produce
    │
    ▼
amg_master
    │
    ▼
amg_deliver

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
java -jar target/ai-music-generation-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key (reserved for future live mode. currently all workers are simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-music-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow amg_music_generation \
  --version 1 \
  --input '{"genre": "sample-genre", "mood": "sample-mood", "durationSec": "sample-durationSec"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w amg_music_generation -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real music tools. MusicGen or Suno for AI composition, DAW APIs for production and mixing, LANDR for automated mastering, and the workflow runs identically in production.

- **ComposeWorker** (`amg_compose`): integrate with MusicGen (Meta), Suno API, or AIVA for AI-powered melody and chord generation with genre-specific training
- **ProduceWorker** (`amg_produce`): use digital audio workstation APIs or audio processing libraries (JFugue, TarsosDSP) for real mixing and effects processing
- **MasterWorker** (`amg_master`): integrate with LANDR API for automated mastering, or use loudness normalization (ITU-R BS.1527) and format-specific encoding (MP3, WAV, FLAC)

The interfaces between workers stay fixed, letting you upgrade any audio processing tool independently.

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
ai-music-generation-ai-music-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aimusicgeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiMusicGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ArrangeWorker.java
│       ├── ComposeWorker.java
│       ├── DeliverWorker.java
│       ├── MasterWorker.java
│       └── ProduceWorker.java
└── src/test/java/aimusicgeneration/workers/
    ├── ComposeWorkerTest.java        # 1 tests
    └── MasterWorkerTest.java        # 1 tests

```
