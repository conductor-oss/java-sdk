# File client design

## Decision

Provider-specific signed transfer behavior remains necessary, but it is implemented as package-private transfer adapters beneath one public `FileClient`. Applications do not select or extend adapters and do not orchestrate multipart sessions.

## Public boundary

`FileClient` exposes five workflow-scoped operations:

```java
String upload(String workflowId, Path source);
String upload(String workflowId, Path source, FileUploadOptions options);
String upload(String workflowId, InputStream source, FileUploadOptions options);
Path download(String workflowId, String fileHandleId, Path destination);
FileMetadata getMetadata(String workflowId, String fileHandleId);
```

Uploads return an opaque `conductor://file/<id>` string. That string is the only file representation stored in workflow input/output. `FileUploadOptions` carries filename, content type, and optional task ID; multipart selection is not caller policy.

## Responsibilities

```text
FileClient
  +-- Conductor API: create, refresh, complete, metadata, multipart lifecycle
  +-- retry classification and interruption
  +-- temporary files and atomic destination replacement
  +-- adapter selection
        +-- S3FileTransferAdapter
        +-- AzureFileTransferAdapter
        +-- GcsFileTransferAdapter
        +-- LocalFileTransferAdapter
        +-- GenericHttpFileTransferAdapter
              +-- SignedUrlHttpTransfer
```

`FileClient` owns orchestration. An adapter performs exactly one upload, ranged part upload, or download attempt. This split keeps URL refresh and retry state in one layer while preserving provider protocol rules.

## Signed HTTP isolation

The Conductor API client and signed-transfer client have different trust boundaries. `FileClient` derives a raw `OkHttpClient` for signed transfers that has:

- no application interceptors or network interceptors;
- no authenticator;
- no cookie jar;
- redirects disabled.

This prevents Conductor credentials or ambient cookies from reaching object-storage hosts. Transfer errors contain operation/status information but not the signed URL.

## Upload paths

Path uploads validate workflow ID, source readability, filename safety, and multipart configuration before creating a server record.

Stream uploads require a filename and copy the stream to a temporary path before server creation. This makes retries repeatable and avoids an orphaned metadata record if buffering fails. The client deletes the temporary path but never closes the caller-owned stream.

## Multipart selection

Multipart is used when:

```text
source size > multipartThreshold AND selected adapter supports multipart
```

Parts are sequential, 1-based, and bounded by the provider-neutral maximum part count. Each ranged body positions a `FileChannel` at the requested offset and must emit exactly the requested length.

| Adapter | Multipart | Completion token |
|---|---|---|
| S3 | Yes | Required non-blank `ETag` response header. |
| Azure | Yes | Deterministic Base64 block ID. |
| GCS | No | — |
| Local | No | — |
| Generic HTTP | No | — |

Azure whole-file upload includes `x-ms-blob-type: BlockBlob`. Azure part upload omits it, adds `comp=block&blockid=...`, and returns the block ID for `commitBlockList`.

## Retry ownership

Adapters never retry. `FileClient` retries transient I/O, HTTP 408/429, expired-signature responses, and 5xx responses up to `retryCount` after the initial attempt. It refreshes the signed URL before every retry. Permanent 4xx responses fail immediately.

Completion is reconciled through workflow-scoped metadata because a lost response is ambiguous. If metadata reports `UPLOADED`, the operation succeeded. A failed multipart session triggers a best-effort server abort without replacing the original failure.

All retry loops check and preserve thread interruption.

## Download safety

Downloads validate the handle and destination before network requests, create missing parent directories, and transfer into a unique sibling temporary file. Success requires a non-null HTTP body and an atomic replace of the destination. Any failure deletes the temporary file and leaves the prior destination untouched.

## Server compatibility

This client requires workflow-aware file routes for upload refresh, completion, metadata, download, and multipart operations. Upload mutation calls must use the exact owning workflow ID; metadata and download calls may use a server-authorized workflow-family member.

The previous smart-file types and task-runner integration are intentionally removed. Mixed worker versions emit incompatible workflow shapes (`{fileHandleId, ...}` versus a raw handle string) and must be deployed in coordination.
