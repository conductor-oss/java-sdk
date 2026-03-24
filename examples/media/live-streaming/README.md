# Live Streaming

Live streaming pipeline: setup, encode, CDN distribute, monitor quality, archive

**Input:** `streamId`, `channelId`, `title`, `resolution` | **Timeout:** 1800s

## Pipeline

```
lsm_setup_stream
    │
lsm_encode_stream
    │
lsm_distribute_stream
    │
lsm_monitor_quality
    │
lsm_archive_stream
```

## Workers

**ArchiveStreamWorker** (`lsm_archive_stream`): Archives the completed stream for VOD access.

Reads `duration`, `title`. Outputs `archiveUrl`, `archiveSize`, `archivedAt`, `thumbnailUrl`.

**DistributeStreamWorker** (`lsm_distribute_stream`): Distributes the encoded stream to CDN edge nodes.

Reads `adaptiveBitrates`. Outputs `playbackUrl`, `cdnNodes`, `viewerCount`, `regions`.

**EncodeStreamWorker** (`lsm_encode_stream`): Encodes the live stream with adaptive bitrate profiles.

Reads `resolution`. Outputs `encodedUrl`, `adaptiveBitrates`, `codec`, `latencyMs`.

**MonitorQualityWorker** (`lsm_monitor_quality`): Monitors stream quality metrics.

Reads `viewerCount`. Outputs `peakViewers`, `avgBitrate`, `bufferRatio`, `duration`, `qualityScore`.

**SetupStreamWorker** (`lsm_setup_stream`): Sets up a live stream with ingest URL and stream key.

Reads `resolution`, `title`. Outputs `ingestUrl`, `streamKey`, `startedAt`.

## Tests

**40 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
