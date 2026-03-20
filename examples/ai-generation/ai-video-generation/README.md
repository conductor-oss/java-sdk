# AI Video Generation in Java Using Conductor :  Script, Storyboard, Generate, Edit, Render

A Java Conductor workflow that produces AI-generated videos through a five-stage production pipeline .  writing the script from a topic, creating a visual storyboard with scene descriptions, generating video clips for each scene, editing clips into a sequence with transitions, and rendering the final video. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the video production pipeline as independent workers ,  you write the video generation logic, Conductor handles sequencing, retries, durability, and observability for free.

## Automated Video Production Is a Multi-Stage Pipeline

Creating a 60-second video about "sustainable energy" requires five distinct production stages: writing a script with narration and scene descriptions, translating the script into a visual storyboard (camera angles, compositions, visual elements per scene), generating video clips for each storyboard frame, editing the clips into a coherent sequence with transitions and timing, and rendering the final output at the target resolution and format.

Each stage is a different skillset and toolset. NLP for scriptwriting, visual planning for storyboarding, diffusion models for video generation, editing algorithms for sequencing, and encoding for rendering. If video generation fails on scene 3 of 5, you need to retry that scene without re-running the scriptwriting and storyboarding that already succeeded.

## The Solution

**You just write the scriptwriting, storyboarding, clip generation, editing, and rendering logic. Conductor handles render retries, stage dependencies, and execution tracking across all video pipeline steps.**

`ScriptWorker` writes the video script from the topic .  narration text, scene descriptions, timing cues, and visual directions for the specified duration and style. `StoryboardWorker` translates each script scene into detailed visual specifications ,  composition, camera movement, color palette, and transition type. `GenerateWorker` creates video clips for each storyboard frame using AI video generation models. `EditWorker` sequences the clips with transitions, timing adjustments, and audio sync. `RenderWorker` encodes the final video at the target resolution, format, and bitrate. Conductor tracks each stage's execution time and output for production analytics.

### What You Write: Workers

Five workers divide the video creation process: scripting, storyboarding, clip generation, editing, and rendering, so each stage can scale and retry independently.

| Worker | Task | What It Does |
|---|---|---|
| **EditWorker** | `avg_edit` | Edits the generated video clips .  applies transitions, audio sync, and timing adjustments to produce a coherent sequence |
| **GenerateWorker** | `avg_generate` | Generates video clips for each storyboard scene using AI video generation in the specified style |
| **RenderWorker** | `avg_render` | Renders the edited video at 1080p resolution, producing the final MP4 output with encoding metadata |
| **ScriptWorker** | `avg_script` | Writes a video script from the topic .  generates scene descriptions, narration, and timing cues for the specified duration |
| **StoryboardWorker** | `avg_storyboard` | Translates the script into a visual storyboard with 5 scenes, shot descriptions, and 15 keyframes |

Workers simulate AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode .  the generation workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
avg_script
    │
    ▼
avg_storyboard
    │
    ▼
avg_generate
    │
    ▼
avg_edit
    │
    ▼
avg_render
```

## Example Output

```
=== Example 807: AI Video Generation. Script, Storyboard, Generate, Edit, Render ===

Step 1: Registering task definitions...
  Registered: avg_script, avg_storyboard, avg_generate, avg_edit, avg_render

Step 2: Registering workflow 'avg_video_generation'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [edit] Processing
  [generate] Processing
  [render] Processing
  [script] Response from OpenAI (LIVE)
  [storyboard] Response from OpenAI (LIVE)

  Status: COMPLETED
  Output: {editedVideoId=..., edits=..., videoId=..., clips=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/ai-video-generation-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live script and storyboard generation (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-video-generation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow avg_video_generation \
  --version 1 \
  --input '{"topic": "sample-topic", "Introduction to quantum computing": "sample-Introduction to quantum computing", "duration": "sample-duration", "style": "sample-style"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w avg_video_generation -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real video stack. GPT-4 for scriptwriting, Runway Gen-2 or Pika Labs for clip generation, FFmpeg for editing and rendering, and the workflow runs identically in production.

- **GenerateWorker** (`avg_generate`): integrate with Runway Gen-2, Pika Labs, or Stable Video Diffusion APIs for real video clip generation from storyboard descriptions
- **EditWorker** (`avg_edit`): use FFmpeg for programmatic video editing with transition effects, audio overlay, and subtitle rendering
- **ScriptWorker** (`avg_script`): use GPT-4 with scriptwriting-specific prompts and templates for proper scene structure, timing, and narration format

Swap any rendering backend or editing tool and the rest of the pipeline continues without modification.

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
ai-video-generation-ai-video-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aivideogeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiVideoGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EditWorker.java
│       ├── GenerateWorker.java
│       ├── RenderWorker.java
│       ├── ScriptWorker.java
│       └── StoryboardWorker.java
└── src/test/java/aivideogeneration/workers/
    ├── RenderWorkerTest.java        # 1 tests
    └── ScriptWorkerTest.java        # 1 tests
```
