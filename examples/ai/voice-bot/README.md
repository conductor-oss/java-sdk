# Voice Bot in Java with Conductor

A Java Conductor workflow that powers a voice-based conversational bot .  transcribing caller audio to text, understanding the caller's intent and extracting entities, generating a contextual response, and synthesizing the response back to speech. Given an audio URL, caller ID, and language, the pipeline produces a spoken reply audio file. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the transcribe-understand-generate-synthesize pipeline.

## Handling Voice Conversations End-to-End

A voice bot needs to process spoken language through four distinct stages: convert audio to text, understand what the caller wants, generate an appropriate response, and convert the response back to speech. Each stage depends on the previous one .  you cannot understand intent without a transcript, and you cannot synthesize speech without a response to speak. Failures at any stage (bad audio, ambiguous intent, synthesis errors) need proper handling and retry logic.

This workflow processes a single voice interaction. The transcriber converts the caller's audio to text. The intent analyzer identifies what the caller wants (e.g., order status, account inquiry) and extracts relevant entities (order numbers, account IDs). The response generator produces a contextual reply based on the detected intent. The synthesizer converts the text response into an audio file that can be played back to the caller.

## The Solution

**You just write the transcription, intent-analysis, response-generation, and speech-synthesis workers. Conductor handles the voice pipeline sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

TranscribeWorker converts audio to text, UnderstandWorker detects intent and extracts entities like order numbers, GenerateWorker crafts a contextual reply, and SynthesizeWorker converts it back to speech.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `vb_generate` | Produces a contextual text response based on the detected intent and extracted entities. |
| **SynthesizeWorker** | `vb_synthesize` | Converts the text response into a speech audio file for playback to the caller. |
| **TranscribeWorker** | `vb_transcribe` | Converts caller audio into text using speech-to-text processing. |
| **UnderstandWorker** | `vb_understand` | Detects the caller's intent (e.g., order_status) and extracts entities (e.g., order numbers). |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
vb_transcribe
    │
    ▼
vb_understand
    │
    ▼
vb_generate
    │
    ▼
vb_synthesize

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
java -jar target/voice-bot-1.0.0.jar

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
java -jar target/voice-bot-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow vb_voice_bot \
  --version 1 \
  --input '{"audioUrl": "https://example.com", "callerId": "TEST-001", "language": "en"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w vb_voice_bot -s COMPLETED -c 5

```

## How to Extend

Each worker handles one voice stage .  connect your STT engine (Google Speech-to-Text, AWS Transcribe, Whisper) for transcription and your TTS engine (Google TTS, Amazon Polly, ElevenLabs) for synthesis, and the voice-bot workflow stays the same.

- **GenerateWorker** (`vb_generate`): use an LLM (GPT-4, Claude) or dialog engine for context-aware response generation
- **SynthesizeWorker** (`vb_synthesize`): integrate with a TTS service (Google Cloud TTS, Amazon Polly, ElevenLabs) for natural speech output
- **TranscribeWorker** (`vb_transcribe`): connect to a speech-to-text API (Google Speech, AWS Transcribe, Whisper) for real audio transcription

Connect Google Speech-to-Text and Amazon Polly and the voice conversation pipeline: transcribe, understand, generate, synthesize, works without modification.

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
voice-bot/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/voicebot/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VoiceBotExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateWorker.java
│       ├── SynthesizeWorker.java
│       ├── TranscribeWorker.java
│       └── UnderstandWorker.java
└── src/test/java/voicebot/workers/
    ├── GenerateWorkerTest.java        # 2 tests
    ├── SynthesizeWorkerTest.java        # 2 tests
    ├── TranscribeWorkerTest.java        # 2 tests
    └── UnderstandWorkerTest.java        # 2 tests

```
