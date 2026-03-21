# Podcast Production Pipeline in Java Using Conductor : Recording, Audio Editing, Transcription, Publishing, and Directory Distribution

## Why Podcast Production Needs Orchestration

Producing a podcast episode involves a multi-stage pipeline where each step transforms or enriches the audio. You ingest the raw recording. capturing duration, sample rate, and channel count. You edit the audio, removing silence, normalizing levels, applying compression, and encoding at the target bitrate to reduce file size. You transcribe the edited audio for show notes, accessibility, and SEO. You publish the episode, generating the RSS feed entry with metadata, duration, and file URLs. Finally, you ping all major podcast directories so the new episode appears in listeners' feeds.

Each stage depends on the previous one. you edit the raw recording (not a published one), you transcribe the edited version (not the raw), and you publish with accurate duration from the edited file. If transcription fails, you still want to publish; but the transcript should be retried independently. Without orchestration, you'd build a monolithic podcast pipeline that mixes FFmpeg commands, speech-to-text API calls, RSS generation, and directory API pings, making it impossible to swap your transcription provider, test editing settings independently, or trace which processing step introduced an audio artifact.

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

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
