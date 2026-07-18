# FileClient Media Transcoder

This example passes videos, thumbnails, and a JSON manifest through a workflow as opaque `conductor://file/<id>` strings. Workers inject `FileClient` and explicitly transfer files; there are no smart file objects or automatic task-runner uploads.

## Cases covered

| Case | Source |
|---|---|
| Upload a caller-owned `InputStream` with required filename and content type | Annotated [`UploadPrimaryVideoWorker.java`](src/main/java/io/conductor/example/mediatranscoder/workers/UploadPrimaryVideoWorker.java) |
| Download a handle, process the file, and upload a `Path` | Annotated [`TranscodeWorker.java`](src/main/java/io/conductor/example/mediatranscoder/workers/TranscodeWorker.java) |
| Download and upload a second derived file in a parallel workflow branch | Annotated [`ThumbnailWorker.java`](src/main/java/io/conductor/example/mediatranscoder/workers/ThumbnailWorker.java) |
| Read metadata and download before producing another handle | Annotated [`ManifestWorker.java`](src/main/java/io/conductor/example/mediatranscoder/workers/ManifestWorker.java) |
| Every public `FileClient` overload in small copyable methods | [`FileClientUsage.java`](src/main/java/io/conductor/example/mediatranscoder/FileClientUsage.java) |
| Pass only handle strings between sequential and parallel tasks | [`media_transcode.json`](src/main/resources/workflow/media_transcode.json) |

Large `Path` uploads use the same call shown in the workers. `FileClient` automatically selects multipart for S3 and Azure when the configured threshold is exceeded; GCS and local storage use one request.

## Annotated worker pattern

Every worker follows the same four-part pattern from [`WorkerTask.java`](../../../conductor-client/src/main/java/com/netflix/conductor/sdk/workflow/task/WorkerTask.java):

1. Inject `FileClient` into a plain Java object.
2. Put the workflow task name on a method with `@WorkerTask`.
3. Receive the resolved task input as a typed object and workflow context with `@WorkflowInstanceIdInputParam`.
4. Return the handle string and use `@OutputParam` to name the task output.

```java
public final class TranscodeWorker {
    private final FileClient files;

    public TranscodeWorker(FileClient files) {
        this.files = files;
    }

    public static class TranscodeInput {
        public String primary_video;
        public String resolution;
    }

    @WorkerTask("transcode_video")
    public @OutputParam("output_file") String transcode(
            TranscodeInput input,
            @WorkflowInstanceIdInputParam String workflowId) throws IOException {
        Path source = Files.createTempFile("source-", ".mov");
        Path output = Files.createTempFile("transcoded-", ".mp4");
        try {
            files.download(workflowId, input.primary_video, source);
            transcode(source, output, input.resolution);
            return files.upload(
                    workflowId,
                    output,
                    new FileUploadOptions().setContentType("video/mp4"));
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(output);
        }
    }
}
```

Register dependency-injected instances directly. `AnnotatedWorkerExecutor` discovers the annotated methods, binds task inputs, maps return values to task outputs, and converts thrown exceptions into failed task results:

```java
AnnotatedWorkerExecutor workers = new AnnotatedWorkerExecutor(taskClient);
workers.initWorkersFromInstances(List.of(
        new UploadPrimaryVideoWorker(fileClient),
        new TranscodeWorker(fileClient),
        new ThumbnailWorker(fileClient),
        new ManifestWorker(fileClient)));
```

The annotation value must exactly match the `name` of the corresponding `SIMPLE` task and its registered task definition. This application checks all four task definitions before starting the workflow, so a missing worker definition cannot silently leave the workflow queued.

## Prerequisites

- Java 21 and Maven 3.8+
- A Conductor server at `http://localhost:8080/api`
- File storage enabled on that server
- The current SDK published to Maven local when running from a source checkout

For local storage, start Conductor with settings equivalent to:

```properties
conductor.file-storage.enabled=true
conductor.file-storage.type=local
```

## Run from this repository

Publish the current client source locally so the standalone Maven example uses the matching API:

```shell
./gradlew :conductor-client:publishToMavenLocal
cd examples/file-storage/media-transcoder
mvn package
CONDUCTOR_SERVER_URL=http://localhost:8080/api \
  java -jar target/media-transcoder-1.0.0.jar
```

The application checks the registered task definitions, creates any missing `upload_primary_video`, `transcode_video`, `extract_thumbnail`, and `create_manifest` definitions, registers `media_transcode`, starts all four workers, and starts an execution. The output contains raw handles for the original video, transcoded video, thumbnail, and manifest.

## Client configuration

Spring applications can tune automatic multipart and retry behavior without changing worker code:

```properties
conductor.file-client.retry-count=3
conductor.file-client.multipart-threshold=104857600
conductor.file-client.multipart-part-size=10485760
```

See the [FileClient guide](../../../docs/file-client.md) for validation, stream ownership, atomic download, and retry details.
