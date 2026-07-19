# FileClient Guide

`FileClient` uploads, downloads, and inspects workflow-scoped binary files. Workflow data contains only opaque strings such as `conductor://file/<id>`; workers never pass storage URLs or SDK file objects to one another.

The Conductor server must have file storage enabled and must expose the workflow-scoped File API. For local setup, use the [Conductor CLI guide](server-setup.md).

## Create a client

```java
import com.netflix.conductor.client.http.ConductorClient;
import org.conductoross.conductor.client.FileClient;

ConductorClient conductor = ConductorClient.builder()
        .basePath("http://localhost:8080/api")
        .build();

FileClient files = new FileClient(conductor);
```

Spring applications can inject the auto-configured `FileClient` bean. Configuration uses `conductor.file-client.*`:

```properties
conductor.file-client.retry-count=3
conductor.file-client.multipart-threshold=104857600
conductor.file-client.multipart-part-size=10485760
```

## Public API

```java
String upload(String workflowId, Path source);
String upload(String workflowId, Path source, FileUploadOptions options);
String upload(String workflowId, InputStream source, FileUploadOptions options);
Path download(String workflowId, String fileHandleId, Path destination);
FileMetadata getMetadata(String workflowId, String fileHandleId);
```

Every call requires workflow context. Upload mutations belong to the exact workflow that created the file. Downloads and metadata reads are available to that workflow's parent/sub-workflow family.

## Upload examples

### Path with inferred filename

```java
Path source = Path.of("/work/input.csv");
String handle = files.upload(workflowId, source);
```

The source must be a readable regular file. `input.csv` becomes the stored filename.

### Path with filename, content type, and task ID

```java
FileUploadOptions options = new FileUploadOptions()
        .setFileName("customer-export.csv")
        .setContentType("text/csv")
        .setTaskId(task.getTaskId());

String handle = files.upload(task.getWorkflowInstanceId(), source, options);
```

`fileName` overrides the path's final segment. Filenames must be simple names, not absolute paths or values containing path separators.

### Caller-owned stream

```java
FileUploadOptions options = new FileUploadOptions()
        .setFileName("events.ndjson")
        .setContentType("application/x-ndjson")
        .setTaskId(task.getTaskId());

try (InputStream source = eventStore.openExport()) {
    String handle = files.upload(task.getWorkflowInstanceId(), source, options);
}
```

A stream upload requires `fileName`. The client buffers the stream to a temporary file before it creates the server record, deletes the temporary file afterward, and does not close the stream. The caller remains responsible for closing it.

### Large file

Application code uses the same path overload for large files:

```java
String handle = files.upload(
        workflowId,
        Path.of("/work/archive.tar"),
        new FileUploadOptions().setContentType("application/x-tar"));
```

Multipart is automatic when the file size is greater than `multipart-threshold` and the provider supports it. S3 uses validated part ETags; Azure uses deterministic block IDs. GCS, local storage, and generic HTTP(S) signed URLs remain single-request. There is no multipart flag on `FileUploadOptions`.

## Metadata example

```java
FileMetadata metadata = files.getMetadata(workflowId, handle);

System.out.printf(
        "%s (%s), %d bytes, hash=%s, status=%s%n",
        metadata.getFileName(),
        metadata.getContentType(),
        metadata.getFileSize(),
        metadata.getContentHash(),
        metadata.getUploadStatus());
```

Metadata reads also support completion reconciliation: an `UPLOADED` status means a completion request succeeded even if its HTTP response was lost.

## Download examples

### New destination

```java
Path destination = Path.of("/work/downloads/input.csv");
Path downloaded = files.download(workflowId, handle, destination);
```

The client creates missing parent directories.

### Replace an existing destination safely

```java
Path existing = Path.of("/work/current-model.bin");
files.download(workflowId, modelHandle, existing);
```

The download first goes to a unique sibling `.part` file. Only a complete transfer atomically replaces `current-model.bin`. Failure removes the partial file and preserves the existing destination. A filesystem that cannot perform atomic replacement is rejected explicitly.

## Recommended annotated worker pattern

```java
public final class ConvertWorker {
    private final FileClient files;

    public ConvertWorker(FileClient files) {
        this.files = files;
    }

    public static class ConvertInput {
        public String document;
    }

    @WorkerTask("convert_document")
    public @OutputParam("document") String convert(
            ConvertInput input,
            @WorkflowInstanceIdInputParam String workflowId) throws IOException {
        Path source = Files.createTempFile("document-", ".bin");
        Path output = Files.createTempFile("converted-", ".pdf");
        try {
            files.download(workflowId, input.document, source);
            convertToPdf(source, output);
            return files.upload(
                    workflowId,
                    output,
                    new FileUploadOptions()
                            .setFileName("converted.pdf")
                            .setContentType("application/pdf"));
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(output);
        }
    }
}
```

`@WorkerTask` names the `SIMPLE` task, the unannotated POJO receives the full resolved input map, `@WorkflowInstanceIdInputParam` supplies the workflow context required by `FileClient`, and `@OutputParam` maps the returned handle to `output.document`. Thrown exceptions become failed task results.

Register already-constructed worker instances so normal constructor injection continues to work:

```java
AnnotatedWorkerExecutor workers = new AnnotatedWorkerExecutor(taskClient);
workers.initWorkersFromInstances(List.of(new ConvertWorker(files)));
```

The annotation value must exactly match both the workflow task's `name` and a registered task definition. The task input and output remain plain strings; no task-runner file integration is required. Implement the lower-level `Worker` interface only when the method-binding model is insufficient and direct access to the full `Task` or `TaskResult` is necessary.

## Retry and security behavior

- A new signed URL is requested before every retry.
- Retries are limited to transient I/O failures, throttling, expired signatures, and server errors.
- Thread interruption is preserved and stops retries.
- Signed URLs are redacted from exceptions.
- Signed requests use a separate raw HTTP client with redirects disabled and no Conductor authentication, cookies, or application interceptors.
- Unknown server storage types can use the generic adapter only when the supplied signed URL is HTTP(S).

See [File client design](../design/file-client.md) for component boundaries and [Media Transcoder](../examples/file-storage/media-transcoder/) for a complete multi-worker workflow.
