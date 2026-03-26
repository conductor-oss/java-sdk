# On-Demand Preview Environments for Feature Branches

Every feature branch needs a review, but reviewing code diffs is not the same as clicking
through the actual app. This workflow provisions an isolated Kubernetes namespace for a
branch (e.g. `feature-auth-v2`), deploys the branch code, configures a preview DNS URL,
and posts the link back to the pull request.

## Workflow

```
branch, repository
      |
      v
+----------------+     +--------------------+     +---------------------+     +--------------+
| fe_provision   | --> | fe_deploy_branch   | --> | fe_configure_dns    | --> | fe_notify    |
+----------------+     +--------------------+     +---------------------+     +--------------+
  PROVISION-1360         deployed to preview        preview URL configured    preview link
  namespace created      environment                                          posted to PR
```

## Workers

**ProvisionWorker** -- Creates a Kubernetes namespace for the branch `feature-auth-v2`.
Returns `provisionId: "PROVISION-1360"`.

**DeployBranchWorker** -- Deploys the branch code to the preview environment. Returns
`deploy_branch: true`.

**ConfigureDnsWorker** -- Configures the preview DNS URL. Returns `configure_dns: true`.

**NotifyWorker** -- Posts the preview link to the pull request so reviewers can click through
the running app. Returns `notify: true`.

## Tests

2 unit tests cover the feature environment pipeline. The workflow timeout is 600 seconds.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
