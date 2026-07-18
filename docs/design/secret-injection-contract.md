# Secret Injection Contract (`runtimeMetadata`)

How worker tools receive declared credentials, end to end. This is the wire
contract shared with the Python SDK (see the Agent SDK Porting Spec, R6) and
implemented server-side by conductor-oss PR #1255 (Conductor OSS builds with
conductor-oss ≥ 3.32.0-rc).

## The contract

1. **Declare** — a tool declares the secret *names* it needs:

   ```java
   @Tool(name = "github_tool", description = "...", credentials = {"GITHUB_TOKEN"})
   public Map<String, Object> run(String query, ToolContext ctx) {
       String token = ctx.getCredential("GITHUB_TOKEN");
       ...
   }
   ```

2. **Stamp** — at worker registration, `WorkerManager` upserts the task
   definition with the declared names on `TaskDef.runtimeMetadata`
   (`List<String>`). The upsert runs on **every** registration (PUT-overwrite
   first, create fallback) so a re-register can never leave a stale or wiped
   stamp.

3. **Resolve + deliver** — a capable host resolves the declared names from its
   secret store *at poll time* and delivers the values on the wire-only
   `Task.runtimeMetadata` map (`Map<String, String>`). Values are never
   persisted to task input/output.

4. **Inject** — before invoking the handler, `WorkerManager` reads the
   delivered values into the thread-local `CredentialContext`; `ToolRegistry`
   snapshots them into the `ToolContext` the tool reads via
   `ctx.getCredential(name)`. The context is cleared in a `finally` on the same
   worker thread — secrets never outlive the call and never cross threads.

## Fail-closed

A declared name the host did **not** deliver fails the task with
`FAILED_WITH_TERMINAL_ERROR` (a config problem — retries are pointless),
naming the missing names and the server capability requirement. Ambient
process env is **never** read as a fallback: a secret named `PATH` that is not
delivered fails the task even though `PATH` exists in the environment.

## What this replaced

Earlier versions fetched secrets per call via `POST /workers/secrets` using a
task context token. That fetch path (and its transport exceptions) is deleted
(porting spec R12): there is no separate fetch call, no execution token, and
no second token authority. `CredentialNotFoundException` remains as the public
`ToolContext.getCredential` miss signal.

## Server capability

| Server | `runtimeMetadata` delivery |
|---|---|
| Conductor OSS before PR #1255 | ✗ (field dropped on registration) |
| Conductor OSS with PR #1255 | ✓ |
| conductor-oss ≥ 3.32.0-rc | ✓ (standalone flavor's secret store is env-backed and read-only via the API) |

The credential e2e suite probes capability first (register a `TaskDef` with
`runtimeMetadata`, read it back) and skips wire-delivery assertions on servers
that drop the field; the write-dependent lifecycle steps additionally skip on
read-only secret stores.
