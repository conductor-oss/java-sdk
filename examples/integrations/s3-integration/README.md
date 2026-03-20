# S3 Integration in Java Using Conductor

A Java Conductor workflow that manages an S3 file upload lifecycle .  uploading an object to a bucket, setting metadata on the uploaded object, generating a presigned URL for temporary access, and notifying a user about the upload with the presigned link. Given a bucket, key, content type, and email, the pipeline produces an upload confirmation, metadata, presigned URL, and notification status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the upload-metadata-presign-notify pipeline.

## Managing S3 Uploads with Metadata, Presigned URLs, and Notifications

Uploading a file to S3 is rarely the end of the story. After the upload, you typically need to tag it with metadata (content type, upload source, processing status), generate a time-limited presigned URL so authorized users can access it without AWS credentials, and notify the intended recipient with the download link. Each step depends on the previous one .  you cannot set metadata on an object that has not been uploaded, and you cannot generate a presigned URL without knowing the bucket and key.

Without orchestration, you would chain S3 SDK calls manually and manage bucket names, keys, and ETags between steps. Conductor sequences the pipeline and passes these values between workers automatically.

## The Solution

**You just write the S3 workers. Object upload, metadata tagging, presigned URL generation, and download notification. Conductor handles upload-to-notify sequencing, S3 API retries, and ETag and key routing between metadata, presign, and notification stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the S3 upload lifecycle: S3UploadWorker stores the object, SetMetadataWorker tags it with attributes, GenerateUrlWorker creates a time-limited presigned URL, and S3NotifyWorker sends the download link to the recipient.

| Worker | Task | What It Does |
|---|---|---|
| **S3UploadWorker** | `s3_upload` | Uploads the object to S3 .  writes the content to the specified bucket and key, returns the ETag and upload confirmation |
| **SetMetadataWorker** | `s3_set_metadata` | Sets metadata on the uploaded object .  tags it with content type, upload source, processing status, and custom metadata |
| **GenerateUrlWorker** | `s3_generate_url` | Generates a presigned URL .  creates a time-limited download URL for the object that works without AWS credentials |
| **S3NotifyWorker** | `s3_notify` | Notifies the user about the upload .  sends the presigned download link to the specified email address |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

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
java -jar target/s3-integration-1.0.0.jar
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
| `AWS_ACCESS_KEY_ID` | _(none)_ | AWS access key ID. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in AWS SDK for production. |
| `AWS_SECRET_ACCESS_KEY` | _(none)_ | AWS secret access key. Required alongside `AWS_ACCESS_KEY_ID` for production use. |
| `S3_BUCKET` | _(none)_ | Default S3 bucket name for uploads. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/s3-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow s3_integration_447 \
  --version 1 \
  --input '{"bucket": "test-value", "key": "test-value", "contentType": "test-value", "notifyEmail": "user@example.com"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w s3_integration_447 -s COMPLETED -c 5
```

## How to Extend

Swap in AWS SDK S3Client.putObject() for uploads, S3Presigner.presignGetObject() for URL generation, and SES or SendGrid for the notification step. The workflow definition stays exactly the same.

- **S3UploadWorker** (`s3_upload`): use the AWS SDK S3Client.putObject() to upload real files to S3
- **GenerateUrlWorker** (`s3_generate_url`): use the AWS SDK S3Presigner to generate real presigned download URLs
- **S3NotifyWorker** (`s3_notify`): integrate with SES, SendGrid, or Slack to send real upload notifications with download links

Connect each worker to the real S3 SDK while maintaining the same output fields, and the upload-to-notification pipeline adapts seamlessly.

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
s3-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/s3integration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── S3IntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateUrlWorker.java
│       ├── S3NotifyWorker.java
│       ├── S3UploadWorker.java
│       └── SetMetadataWorker.java
└── src/test/java/s3integration/workers/
    ├── GenerateUrlWorkerTest.java        # 2 tests
    ├── S3NotifyWorkerTest.java        # 2 tests
    ├── S3UploadWorkerTest.java        # 2 tests
    └── SetMetadataWorkerTest.java        # 2 tests
```
