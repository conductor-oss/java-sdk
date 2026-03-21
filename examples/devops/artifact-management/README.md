# Artifact Management in Java with Conductor : Build, Sign, Publish, Cleanup

Build artifact lifecycle orchestration: build, sign, publish, and cleanup old artifacts. ## Build Artifacts Need a Managed Lifecycle

After building a JAR, Docker image, or npm package, the artifact needs signing (so consumers can verify it hasn't been tampered with), publishing to a repository (Artifactory, Docker Hub, npm registry), and old versions need cleanup (keeping the last 10 versions, deleting artifacts older than 90 days). Without lifecycle management, artifact repositories grow unbounded, unsigned artifacts create supply chain risks, and there's no audit trail of what was published when.

Each step has dependencies: signing requires the build output, publishing requires the signed artifact, and cleanup requires knowing which versions are still in use. If publishing fails, the artifact is built and signed but not available. retrying should resume from publishing, not rebuild.

## The Solution

**You write the build, sign, and publish logic. Conductor handles artifact lifecycle sequencing, provenance tracking, and retention enforcement.**

`BuildWorker` compiles the source code and produces the build artifact. JAR, Docker image, or package. with version metadata. `SignWorker` signs the artifact for integrity verification using GPG, Sigstore, or Docker Content Trust. `PublishWorker` uploads the signed artifact to the target repository with version tags and metadata. `CleanupWorker` applies retention policies, removing old versions, cleaning up unused tags, and reclaiming storage. Conductor sequences these steps and records the complete artifact provenance chain.

### What You Write: Workers

Four workers manage the artifact lifecycle. Building the package, signing for integrity, publishing to a repository, and enforcing retention cleanup.

| Worker | Task | What It Does |
|---|---|---|
| **BuildWorker** | `am_build` | Builds the project artifact. |
| **CleanupWorker** | `am_cleanup` | Cleans up old artifacts beyond retention policy. |
| **PublishWorker** | `am_publish` | Publishes the signed artifact to Artifactory. |
| **SignWorker** | `am_sign` | Signs the built artifact with GPG key. |

the workflow and rollback logic stay the same.

### The Workflow

```
am_build
 │
 ▼
am_sign
 │
 ▼
am_publish
 │
 ▼
am_cleanup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
