# Deploying Configuration with Schema Validation and Hash Verification

Pushing a config change to 8 nodes without validation risks inconsistent state across the
cluster. This workflow loads the config (with settings like `dbPoolSize: 20`,
`cacheEnabled: true`, `logLevel: "info"`, `maxRetries: 3`, `timeoutMs: 5000`), validates
it against schema `"app-config-v2"`, deploys to 8 nodes, and verifies all nodes have the
same `configHash`.

## Workflow

```
configSource, environment, configData
                 |
                 v
+------------------+     +----------------------+     +--------------------+     +--------------------+
| cf_load_config   | --> | cf_validate_config   | --> | cf_deploy_config   | --> | cf_verify_config   |
+------------------+     +----------------------+     +--------------------+     +--------------------+
  config loaded:           valid: true                  8 nodes updated          allNodesConsistent:
  dbPoolSize=20            warning: logLevel            deploymentId assigned     true
  version: 1.4.0           may be verbose                                        configHash checked
```

## Workers

**LoadConfigWorker** -- Loads config from `configSource` for `environment`. Returns config
map with `dbPoolSize: 20`, `cacheEnabled: true`, `logLevel: "info"`, `maxRetries: 3`,
`timeoutMs: 5000`. Schema is `"app-config-v2"`, version `"1.4.0"`.

**ValidateConfigWorker** -- Validates against the schema. Returns `valid: true`, empty
`errors`, and a warning: `"logLevel 'info' may be verbose for production"`.

**DeployConfigWorker** -- Deploys to `nodesUpdated: 8`. Returns a `deploymentId`.

**VerifyConfigWorker** -- Verifies the deployment: `allNodesConsistent: true`,
`configHash: "sha256:abc123def456"`.

## Tests

12 unit tests cover config loading, validation, deployment, and consistency verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
