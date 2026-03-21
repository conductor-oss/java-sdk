# AI Voice Cloning in Java Using Conductor :  Sample Collection, Model Training, Speech Generation, Verification

A Java Conductor workflow that clones a speaker's voice .  collecting voice samples from the target speaker, training a voice model on those samples, generating speech in the cloned voice from target text, verifying the output against the original voice for quality and similarity, and delivering the final audio. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage voice cloning pipeline as independent workers ,  you write the voice processing logic, Conductor handles sequencing, retries, durability, and observability.

## Voice Cloning Requires Training, Generation, and Verification

Creating synthetic speech that sounds like a specific person requires three phases: training (analyzing voice samples to capture pitch, tone, cadence, and pronunciation patterns), generation (producing new speech from text using the trained model), and verification (comparing the synthetic output to the original voice for similarity and naturalness).

Sample collection needs sufficient audio quality and duration. Training can take minutes to hours depending on model architecture. Generation must handle the target language's phonetics. Verification must catch both quality issues (robotic artifacts, mispronunciation) and similarity gaps (wrong pitch, missing vocal characteristics). If verification fails, you need to retrain or adjust generation parameters without recollecting samples.

## The Solution

**You just write the sample collection, voice model training, speech synthesis, verification, and delivery logic. Conductor handles training retries, pipeline sequencing, and verification tracking across all synthesis steps.**

`CollectSamplesWorker` gathers and validates voice samples from the speaker .  checking audio quality, duration, and language coverage. `TrainModelWorker` trains the voice cloning model on the collected samples, producing a speaker embedding or fine-tuned model. `GenerateWorker` synthesizes speech from the target text using the trained voice model. `VerifyWorker` compares the generated speech against the original samples for similarity (MOS score, speaker verification) and quality (naturalness, intelligibility). `DeliverWorker` packages the verified audio with metadata. Conductor tracks the full pipeline and records verification scores for quality monitoring.

### What You Write: Workers

The voice cloning pipeline decomposes into discrete workers for sample collection, model training, speech synthesis, and verification, keeping each concern isolated.

| Worker | Task | What It Does |
|---|---|---|
| **CollectSamplesWorker** | `avc_collect_samples` | Collects 25 voice samples (12 minutes total) from the target speaker for model training |
| **DeliverWorker** | `avc_deliver` | Delivers the verified audio as MP3 format to the output destination |
| **GenerateWorker** | `avc_generate` | Synthesizes speech from target text using the trained voice model, producing audio with duration metadata |
| **TrainModelWorker** | `avc_train_model` | Trains Model and returns model id, epochs, loss |
| **VerifyWorker** | `avc_verify` | Verifies the generated speech against the original voice .  measures similarity (0.96) and naturalness (0.91) scores |

Workers simulate AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode .  the generation workflow stays the same.

### The Workflow

```
avc_collect_samples
    │
    ▼
avc_train_model
    │
    ▼
avc_generate
    │
    ▼
avc_verify
    │
    ▼
avc_deliver

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
java -jar target/ai-voice-cloning-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live voice verification (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-voice-cloning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow avc_voice_cloning \
  --version 1 \
  --input '{"speakerId": "TEST-001", "targetText": "Process this order for customer C-100", "language": "en"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w avc_voice_cloning -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real voice stack. ElevenLabs or Coqui TTS for model training and synthesis, Resemblyzer for speaker verification scoring, your audio CDN for delivery, and the workflow runs identically in production.

- **TrainModelWorker** (`avc_train_model`): integrate with ElevenLabs Voice Cloning API, Coqui TTS voice cloning, or OpenAI's voice model fine-tuning for real voice model training
- **GenerateWorker** (`avc_generate`): use ElevenLabs Text-to-Speech, Azure Neural TTS with custom voice, or Bark for multilingual voice generation
- **VerifyWorker** (`avc_verify`): implement speaker verification using Resemblyzer for voice similarity scoring, PESQ for speech quality metrics, and manual A/B testing workflows

Replace any synthesis engine or verification model and the surrounding workers continue unchanged.

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
ai-voice-cloning-ai-voice-cloning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aivoicecloning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiVoiceCloningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectSamplesWorker.java
│       ├── DeliverWorker.java
│       ├── GenerateWorker.java
│       ├── TrainModelWorker.java
│       └── VerifyWorker.java
└── src/test/java/aivoicecloning/workers/
    ├── CollectSamplesWorkerTest.java        # 1 tests
    └── VerifyWorkerTest.java        # 1 tests

```
