# Audio Transcription in Java Using Conductor :  Speech-to-Text, Speaker Diarization, and Keyword Extraction

A Java Conductor workflow example for audio transcription pipelines: preprocessing raw audio, running speech-to-text with speaker diarization, generating word-level timestamps, and extracting keywords. Uses [Conductor](https://github.

## The Problem

You need to turn raw audio recordings into structured, searchable text. That means normalizing audio levels and reducing background noise, running speech-to-text transcription with speaker identification, aligning words to timestamps so you can jump to any point in the recording, and extracting keywords for indexing and search. Each step depends on the output of the one before it. You can't generate timestamps without a transcript, and you can't transcribe without clean audio.

Without orchestration, you'd chain all of this in a single monolithic class. Calling FFmpeg for preprocessing, then a speech-to-text API, then a timestamping pass, then NLP for keyword extraction. If the transcription API times out, you'd need manual retry logic. If the process crashes after a 45-minute transcription completes but before keywords are extracted, you'd lose all that work and start over. Adding a new post-processing step (like sentiment analysis or topic detection) means rewriting the pipeline.

## The Solution

**You just write the audio processing workers.  preprocessing, transcription, timestamping, keyword extraction. Conductor handles sequential execution, crash recovery mid-transcription, and automatic retries when speech APIs time out.**

Each stage of the transcription pipeline is a simple, independent worker, a plain Java class that does one thing. The preprocessing worker normalizes audio. The transcription worker converts speech to text. The timestamp worker aligns words to time codes. The keyword worker extracts topics. Conductor executes them in sequence, passes each worker's output to the next, retries if a speech API call fails, and resumes from exactly where it left off if the process crashes mid-transcription. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

This pipeline breaks audio processing into four focused workers. from raw audio cleanup through speech-to-text to keyword tagging, each handling one stage of the transcription chain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractKeywordsWorker** | `au_extract_keywords` | Extracts keywords from the transcript. |
| **GenerateTimestampsWorker** | `au_generate_timestamps` | Generates timestamps for transcript segments. |
| **PreprocessAudioWorker** | `au_preprocess_audio` | Preprocesses audio: normalization, noise reduction. |
| **TranscribeWorker** | `au_transcribe` | Transcribes audio into text with speaker diarization. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
au_preprocess_audio
    │
    ▼
au_transcribe
    │
    ▼
au_generate_timestamps
    │
    ▼
au_extract_keywords

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
java -jar target/audio-transcription-1.0.0.jar

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
java -jar target/audio-transcription-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow audio_transcription \
  --version 1 \
  --input '{"audioUrl": "https://example.com", "language": "en", "speakerCount": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w audio_transcription -s COMPLETED -c 5

```

## How to Extend

Each worker maps directly to a real audio service .  swap in Whisper for transcription, FFmpeg for preprocessing, or AWS Comprehend for keyword extraction, and the workflow runs identically without any changes.

- **PreprocessAudioWorker** → shell out to FFmpeg for normalization, noise reduction, and format conversion (`ffmpeg -i input.wav -af "loudnorm,afftdn" output.wav`)
- **TranscribeWorker** → call Whisper API, Google Speech-to-Text, or AWS Transcribe for real speech-to-text with speaker diarization
- **GenerateTimestampsWorker** → use Whisper's word-level timestamps or forced alignment tools like `aeneas` to map each word to its position in the audio
- **ExtractKeywordsWorker** → integrate a real NLP service (spaCy, AWS Comprehend, or OpenAI) for keyword and topic extraction

As long as each worker produces the expected audio metadata and text fields, the pipeline flows seamlessly from preprocessing through keyword extraction.

**Add new pipeline stages** by creating a new worker and adding a task to `workflow.json`, for example, sentiment analysis per speaker segment, automatic summarization, or translation to other languages.

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
audio-transcription/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/audiotranscription/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AudioTranscriptionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractKeywordsWorker.java
│       ├── GenerateTimestampsWorker.java
│       ├── PreprocessAudioWorker.java
│       └── TranscribeWorker.java
└── src/test/java/audiotranscription/workers/
    ├── ExtractKeywordsWorkerTest.java        # 6 tests
    ├── GenerateTimestampsWorkerTest.java        # 6 tests
    ├── PreprocessAudioWorkerTest.java        # 6 tests
    └── TranscribeWorkerTest.java        # 6 tests

```
