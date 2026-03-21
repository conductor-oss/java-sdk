# Multimodal RAG in Java Using Conductor : Text, Image, and Audio Processing in Parallel

## When Questions Include More Than Text

Users don't just ask text questions. A support ticket might include a screenshot of an error dialog, a voice memo describing the problem, and a text description. A product review might have photos alongside written feedback. Answering these questions requires processing each modality. extracting text embeddings, image features (via CLIP or a vision model), and audio features (via Whisper transcription), then searching a multimodal index that spans all content types.

Each modality processor is independent: text embedding can run simultaneously with image feature extraction and audio transcription. But all three must complete before the multimodal search can run. If the image processor fails (corrupt image, model timeout), you need to retry it without re-processing the text and audio that already succeeded.

Without orchestration, parallel multimodal processing means managing thread pools for heterogeneous workloads (CPU-bound text embedding, GPU-bound image processing, API-bound audio transcription), handling partial failures, and synchronizing results before search.

## The Solution

**You write the modality-specific processors and cross-modal search logic. Conductor handles the parallel processing, retries, and observability.**

Each modality processor is an independent worker. text embedding, image feature extraction, audio processing. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A multimodal search worker combines all feature vectors for a cross-modal search, and a generation worker produces an answer citing text, image, and audio sources. If the audio processor times out, Conductor retries it independently.

### What You Write: Workers

Six workers handle multi-modal content. detecting input modality, processing text, image, and audio in parallel via FORK_JOIN, searching across all modalities, and generating an answer from the unified multi-modal context.

| Worker | Task | What It Does |
|---|---|---|
| **DetectModalityWorker** | `mm_detect_modality` | Worker that detects modalities from a question and its attachments. Returns detected modalities (text, image, audio),... |
| **GenerateWorker** | `mm_generate` | Worker that generates a final answer from the question, multimodal search results, and detected modalities. Produces ... |
| **MultimodalSearchWorker** | `mm_multimodal_search` | Worker that performs a multimodal search using text embeddings, image features, and audio features. Returns ranked se... |
| **ProcessAudioWorker** | `mm_process_audio` | Worker that processes audio references by extracting audio features. Returns a list of features for each audio clip, ... |
| **ProcessImageWorker** | `mm_process_image` | Worker that processes image references by extracting visual features. Returns a list of features for each image, incl... |
| **ProcessTextWorker** | `mm_process_text` | Worker that processes text content by generating an embedding vector and extracting keywords. Returns an 8-dimensiona... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
