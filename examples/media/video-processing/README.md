# Video Processing

Video workflow: upload, transcode to multiple formats, generate thumbnails, extract metadata, and publish.

**Input:** `videoId`, `sourceUrl`, `title`, `creatorId` | **Timeout:** 60s

## Pipeline

```
vid_upload
    │
vid_transcode
    │
vid_thumbnail
    │
vid_metadata
    │
vid_publish
```

## Workers

**MetadataWorker** (`vid_metadata`)

Reads `duration`, `resolutions`, `title`, `videoId`. Outputs `metadata`.

**PublishWorker** (`vid_publish`)

Reads `hlsUrl`, `title`, `videoId`. Outputs `publishUrl`, `publishedAt`, `status`.

**ThumbnailWorker** (`vid_thumbnail`): Generates video thumbnail. Uses FFmpeg if available, otherwise creates thumbnail via java.awt.

```java
int captureAt = duration / 3;
g.drawString("Video: " + videoId, 100, height / 2);
```

Reads `duration`, `videoId`. Outputs `thumbnailUrl`, `capturedAtSecond`, `width`, `height`, `generated`.

**TranscodeWorker** (`vid_transcode`): Transcodes video. Tries real FFmpeg if available, otherwise uses java.awt for image processing fallback.

```java
ffmpegAvailable = p.waitFor() == 0;
```

Reads `storagePath`, `videoId`. Outputs `resolutions`, `hlsUrl`, `format`, `method`.

**UploadWorker** (`vid_upload`): Ingests video upload. Real file validation and metadata extraction.

```java
fileSizeMb = fileExists ? f.length() / (1024 * 1024) : 0;
fileSizeMb = 100 + Math.abs(sourceUrl.hashCode() % 900);
```

Reads `sourceUrl`, `videoId`. Outputs `storagePath`, `duration`, `fileSizeMb`, `codec`, `uploadedAt`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
