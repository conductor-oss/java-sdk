# Multimodal RAG: Processing Text, Images, and Audio in Parallel

A question arrives with attached images and audio files. Plain text RAG can't handle these -- images need object detection, audio needs transcription, and the text needs embedding. This workflow detects all modalities, processes them in parallel via FORK_JOIN, runs a multimodal search across the combined features, and generates a unified answer.

## Workflow

```
question, attachments
       │
       ▼
┌──────────────────────┐
│ mm_detect_modality   │  Identify text, image, audio
└──────────┬───────────┘
           │  modalities, imageRefs, audioRefs
           ▼
┌─── FORK_JOIN ─────────────────────────────────────┐
│                    │                               │
│ ┌────────────────┐ │ ┌─────────────────┐          │ ┌──────────────────┐
│ │mm_process_text │ │ │mm_process_image │          │ │mm_process_audio  │
│ └────────────────┘ │ └─────────────────┘          │ └──────────────────┘
└────────┬───────────┴─────────────┬────────────────┘
         ▼                         ▼
    ┌──────────┐
    │   JOIN   │
    └────┬─────┘
         ▼
┌────────────────────────┐
│ mm_multimodal_search   │  Search across combined features
└────────┬───────────────┘
         ▼
┌────────────────────────┐
│ mm_generate            │  Generate answer from all modalities
└────────────────────────┘
```

## Workers

**DetectModalityWorker** (`mm_detect_modality`) -- Returns detected modalities `["text", "image", "audio"]`. Provides image references with IDs like `"img-001"`, URLs at `storage.example.com`, and descriptions. Provides audio references similarly.

**ProcessTextWorker** (`mm_process_text`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls OpenAI Embeddings at `/v1/embeddings`. Returns keywords `["multimodal", "search", "retrieval", "embedding", "analysis"]` and the embedding vector.

**ProcessImageWorker** (`mm_process_image`) -- Returns image features including detected objects like `["diagram", "workflow", "arrows", "nodes"]` and descriptions for each image reference.

**ProcessAudioWorker** (`mm_process_audio`) -- Returns audio features including transcription and extracted entities for each audio reference.

**MultimodalSearchWorker** (`mm_multimodal_search`) -- Returns 4 hardcoded search results combining information across all modalities with relevance scores.

**GenerateWorker** (`mm_generate`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `gpt-4o-mini` with the combined multimodal context. Returns the generated answer incorporating text, image, and audio information.

## Tests

38 tests extensively cover modality detection, each processing pipeline, multimodal search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
