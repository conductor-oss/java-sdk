# Build-Sign-Publish Pipeline with Artifact Retention

Every release needs a built artifact, a GPG signature, a publish to Artifactory, and cleanup
of stale versions that exceed your retention policy. Doing these steps manually invites
mistakes -- an unsigned JAR shipped to production, or old artifacts eating disk forever. This
workflow chains the four stages so nothing is skipped.

## Workflow

```
project, version
       |
       v
 +-----------+     +-----------+     +-------------+     +-------------+
 | am_build  | --> | am_sign   | --> | am_publish  | --> | am_cleanup  |
 +-----------+     +-----------+     +-------------+     +-------------+
   BUILD-1355        GPG signed       Artifactory         5 old artifacts
   success=true      processed=true   publish=true        removed
```

## Workers

**BuildWorker** -- Reads `project` and `version` (defaults to `"unknown"` / `"0.0.0"`).
Produces a deterministic `buildId` of `"BUILD-1355"` and `success: true`.

**SignWorker** -- Receives the build output via `signData`. Signs the artifact with a GPG key
and returns `sign: true`, `processed: true`.

**PublishWorker** -- Receives the sign output via `publishData`. Publishes the signed artifact
to Artifactory and returns `publish: true`, `processed: true`.

**CleanupWorker** -- Runs after publish. Removes 5 old artifacts beyond the retention policy.
Returns `cleanup: true` with a `completedAt` ISO-8601 timestamp.

## Tests

17 unit tests cover build, signing, publishing, and cleanup scenarios.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
