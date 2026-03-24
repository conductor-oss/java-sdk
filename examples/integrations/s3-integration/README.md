# S3 Integration

Orchestrates s3 integration through a multi-stage Conductor workflow.

**Input:** `bucket`, `key`, `contentType`, `notifyEmail` | **Timeout:** 60s

## Pipeline

```
s3_upload
    │
s3_set_metadata
    │
s3_generate_url
    │
s3_notify
```

## Workers

**GenerateUrlWorker** (`s3_generate_url`): Generates a presigned URL for an S3 object.

Reads `bucket`, `expiresIn`, `key`. Outputs `presignedUrl`, `expiresAt`.

**S3NotifyWorker** (`s3_notify`): Notifies a user about a file upload.

Reads `email`, `key`. Outputs `notified`, `sentAt`.

**S3UploadWorker** (`s3_upload`): Uploads an object to S3.

Reads `bucket`, `contentType`, `key`. Outputs `etag`, `versionId`, `size`.

**SetMetadataWorker** (`s3_set_metadata`): Sets metadata on an S3 object.

Reads `etag`, `key`. Outputs `metadata`, `updated`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
