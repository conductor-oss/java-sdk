# Managing Release 3.2.0 from Preparation to Announcement

A release involves 12 changes and 3 fixes, but without a formal process someone deploys
before the release manager approves and stakeholders learn about it from a customer tweet.
This workflow prepares the release, gates it on approval, deploys to production, and
publishes release notes.

## Workflow

```
version, product
      |
      v
+--------------+     +---------------+     +--------------+     +----------------+
| rm_prepare   | --> | rm_approve    | --> | rm_deploy    | --> | rm_announce    |
+--------------+     +---------------+     +--------------+     +----------------+
  PREPARE-1500        approved by          deployed to          release notes
  12 changes,         release manager      production           published
  3 fixes
```

## Workers

**PrepareWorker** -- Prepares release 3.2.0 for the platform: 12 changes, 3 fixes. Returns
`prepareId: "PREPARE-1500"`.

**ApproveWorker** -- Gates the release on release manager approval. Returns `approve: true`.

**DeployWorker** -- Deploys the approved version to production. Returns `deploy: true`.

**AnnounceWorker** -- Publishes release notes and notifies stakeholders. Returns
`announce: true`.

## Tests

2 unit tests cover the release management pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
