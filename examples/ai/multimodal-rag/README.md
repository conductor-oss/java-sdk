# Multimodal RAG in Java Using Conductor :  Text, Image, and Audio Processing in Parallel

A Java Conductor workflow that handles questions with mixed-media attachments .  detecting which modalities are present (text, images, audio), processing each modality in parallel (text embedding, image feature extraction, audio transcription), searching across a multimodal index, and generating an answer that incorporates all modalities. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate modality detection, parallel processing, search, and generation as independent workers ,  you write the modality-specific logic, Conductor handles parallelism, retries, durability, and observability.

## When Questions Include More Than Text

Users don't just ask text questions. A support ticket might include a screenshot of an error dialog, a voice memo describing the problem, and a text description. A product review might have photos alongside written feedback. Answering these questions requires processing each modality .  extracting text embeddings, image features (via CLIP or a vision model), and audio features (via Whisper transcription) ,  then searching a multimodal index that spans all content types.

Each modality processor is independent: text embedding can run simultaneously with image feature extraction and audio transcription. But all three must complete before the multimodal search can run. If the image processor fails (corrupt image, model timeout), you need to retry it without re-processing the text and audio that already succeeded.

Without orchestration, parallel multimodal processing means managing thread pools for heterogeneous workloads (CPU-bound text embedding, GPU-bound image processing, API-bound audio transcription), handling partial failures, and synchronizing results before search.

## The Solution

**You write the modality-specific processors and cross-modal search logic. Conductor handles the parallel processing, retries, and observability.**

Each modality processor is an independent worker .  text embedding, image feature extraction, audio processing. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A multimodal search worker combines all feature vectors for a cross-modal search, and a generation worker produces an answer citing text, image, and audio sources. If the audio processor times out, Conductor retries it independently.

### What You Write: Workers

Six workers handle multi-modal content .  detecting input modality, processing text, image, and audio in parallel via FORK_JOIN, searching across all modalities, and generating an answer from the unified multi-modal context.

| Worker | Task | What It Does |
|---|---|---|
| **DetectModalityWorker** | `mm_detect_modality` | Worker that detects modalities from a question and its attachments. Returns detected modalities (text, image, audio),... |
| **GenerateWorker** | `mm_generate` | Worker that generates a final answer from the question, multimodal search results, and detected modalities. Produces ... |
| **MultimodalSearchWorker** | `mm_multimodal_search` | Worker that performs a multimodal search using text embeddings, image features, and audio features. Returns ranked se... |
| **ProcessAudioWorker** | `mm_process_audio` | Worker that processes audio references by extracting audio features. Returns a list of features for each audio clip, ... |
| **ProcessImageWorker** | `mm_process_image` | Worker that processes image references by extracting visual features. Returns a list of features for each image, incl... |
| **ProcessTextWorker** | `mm_process_text` | Worker that processes text content by generating an embedding vector and extracting keywords. Returns an 8-dimensiona... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
mm_detect_modality
    │
    ▼
FORK_JOIN
    ├── mm_process_text
    ├── mm_process_image
    └── mm_process_audio
    │
    ▼
JOIN (wait for all branches)
mm_multimodal_search
    │
    ▼
mm_generate
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
java -jar target/multimodal-rag-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use simulated responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/multimodal-rag-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multimodal_rag_workflow \
  --version 1 \
  --input '{"question": "test-value", "attachments": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multimodal_rag_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker processes one modality .  swap in CLIP for image features, Whisper for audio transcription, OpenAI Embeddings for text, and a multimodal LLM like GPT-4V for generation, and the parallel processing pipeline runs unchanged.

- **DetectModalityWorker** (`mm_detect_modality`): add MIME type detection and content extraction for PDFs, videos, or other media types
- **ProcessTextWorker** (`mm_process_text`): swap in a real embedding model (OpenAI, Cohere) for text vectorization
- **ProcessImageWorker** (`mm_process_image`): integrate CLIP, GPT-4 Vision, or a custom CNN for image feature extraction
- **ProcessAudioWorker** (`mm_process_audio`): integrate Whisper or Google Speech-to-Text for audio transcription and feature extraction
- **MultimodalSearchWorker** (`mm_multimodal_search`): connect to a multimodal vector store that supports cross-modal retrieval
- **GenerateWorker** (`mm_generate`): swap in a multimodal LLM (GPT-4V, Gemini Pro Vision) for answers that reference images and audio

Each modality processor returns the same embedding/metadata shape, so adding new modalities (video, PDF) requires only a new processor worker and fork branch.

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
multimodal-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multimodalrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultimodalRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectModalityWorker.java
│       ├── GenerateWorker.java
│       ├── MultimodalSearchWorker.java
│       ├── ProcessAudioWorker.java
│       ├── ProcessImageWorker.java
│       └── ProcessTextWorker.java
└── src/test/java/multimodalrag/workers/
    ├── DetectModalityWorkerTest.java        # 7 tests
    ├── GenerateWorkerTest.java        # 7 tests
    ├── MultimodalSearchWorkerTest.java        # 6 tests
    ├── ProcessAudioWorkerTest.java        # 6 tests
    ├── ProcessImageWorkerTest.java        # 6 tests
    └── ProcessTextWorkerTest.java        # 6 tests
```
