# Podcast Workflow

Orchestrates podcast workflow through a multi-stage Conductor workflow.

**Input:** `episodeId`, `showName`, `episodeTitle`, `hostId` | **Timeout:** 60s

## Pipeline

```
pod_record
    │
pod_edit
    │
pod_transcribe
    │
pod_publish
    │
pod_distribute
```

## Workers

**DistributeWorker** (`pod_distribute`)

Reads `directories`. Outputs `directories`, `pingsSent`, `distributedAt`.

**EditWorker** (`pod_edit`)

Reads `editedAudioUrl`. Outputs `editedAudioUrl`, `finalDuration`, `fileSizeMb`, `bitrate`, `silenceRemoved`.

**PublishWorker** (`pod_publish`)

Reads `episodeUrl`. Outputs `episodeUrl`, `rssUrl`, `publishedAt`.

**RecordWorker** (`pod_record`)

Reads `rawAudioUrl`. Outputs `rawAudioUrl`, `durationMinutes`, `sampleRate`, `channels`.

**TranscribeWorker** (`pod_transcribe`)

Reads `transcriptUrl`. Outputs `transcriptUrl`, `wordCount`, `language`, `confidence`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
