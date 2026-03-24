# Audio Transcription in Java Using Conductor : Speech-to-Text, Speaker Diarization, and Keyword Extraction

## The Problem

You need to turn raw audio recordings into structured, searchable text. That means normalizing audio levels and reducing background noise, running speech-to-text transcription with speaker identification, aligning words to timestamps so you can jump to any point in the recording, and extracting keywords for indexing and search. Each step depends on the output of the one before it. You can't generate timestamps without a transcript, and you can't transcribe without clean audio.

Without orchestration, you'd chain all of this in a single monolithic class. Calling FFmpeg for preprocessing, then a speech-to-text API, then a timestamping pass, then NLP for keyword extraction. If the transcription API times out, you'd need manual retry logic. If the process crashes after a 45-minute transcription completes but before keywords are extracted, you'd lose all that work and start over. Adding a new post-processing step (like sentiment analysis or topic detection) means rewriting the pipeline.

## The Solution

**You just write the audio processing workers. preprocessing, transcription, timestamping, keyword extraction. Conductor handles sequential execution, crash recovery mid-transcription, and automatic retries when speech APIs time out.**

Each stage of the transcription pipeline is a simple, independent worker, a plain Java class that does one thing. The preprocessing worker normalizes audio. The transcription worker converts speech to text. The timestamp worker aligns words to time codes. The keyword worker extracts topics. Conductor executes them in sequence, passes each worker's output to the next, retries if a speech API call fails, and resumes from exactly where it left off if the process crashes mid-transcription.

### What You Write: Workers

This pipeline breaks audio processing into four focused workers. from raw audio cleanup through speech-to-text to keyword tagging, each handling one stage of the transcription chain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractKeywordsWorker** | `au_extract_keywords` | Extracts keywords from the transcript. |
| **GenerateTimestampsWorker** | `au_generate_timestamps` | Generates timestamps for transcript segments. |
| **PreprocessAudioWorker** | `au_preprocess_audio` | Preprocesses audio: normalization, noise reduction. |
| **TranscribeWorker** | `au_transcribe` | Transcribes audio into text with speaker diarization. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
