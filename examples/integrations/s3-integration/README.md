# S3 Integration in Java Using Conductor

## Managing S3 Uploads with Metadata, Presigned URLs, and Notifications

Uploading a file to S3 is rarely the end of the story. After the upload, you typically need to tag it with metadata (content type, upload source, processing status), generate a time-limited presigned URL so authorized users can access it without AWS credentials, and notify the intended recipient with the download link. Each step depends on the previous one. you cannot set metadata on an object that has not been uploaded, and you cannot generate a presigned URL without knowing the bucket and key.

Without orchestration, you would chain S3 SDK calls manually and manage bucket names, keys, and ETags between steps. Conductor sequences the pipeline and passes these values between workers automatically.

## The Solution

**You just write the S3 workers. Object upload, metadata tagging, presigned URL generation, and download notification. Conductor handles upload-to-notify sequencing, S3 API retries, and ETag and key routing between metadata, presign, and notification stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the S3 upload lifecycle: S3UploadWorker stores the object, SetMetadataWorker tags it with attributes, GenerateUrlWorker creates a time-limited presigned URL, and S3NotifyWorker sends the download link to the recipient.

| Worker | Task | What It Does |
|---|---|---|
| **S3UploadWorker** | `s3_upload` | Uploads the object to S3. writes the content to the specified bucket and key, returns the ETag and upload confirmation |
| **SetMetadataWorker** | `s3_set_metadata` | Sets metadata on the uploaded object. tags it with content type, upload source, processing status, and custom metadata |
| **GenerateUrlWorker** | `s3_generate_url` | Generates a presigned URL. creates a time-limited download URL for the object that works without AWS credentials |
| **S3NotifyWorker** | `s3_notify` | Notifies the user about the upload. sends the presigned download link to the specified email address |

the workflow orchestration and error handling stay the same.

### The Workflow

```
s3_upload
 │
 ▼
s3_set_metadata
 │
 ▼
s3_generate_url
 │
 ▼
s3_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
