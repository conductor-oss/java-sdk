# CI/CD Pipeline in Java with Conductor: Build, Parallel Tests, Deploy Staging, Deploy Production

Someone pushed to main. Seven CI jobs kicked off in three different systems. The unit tests passed, but the integration test job silently timed out and reported "success" because the exit code wasn't checked. The security scan found a critical vulnerability in a transitive dependency, but it posted to a Slack channel nobody monitors. Meanwhile, the deploy job grabbed a stale artifact from the previous run because two builds wrote to the same S3 path. Production is now running code that failed integration tests, with a known CVE, and the deploy log says "all green." This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full CI/CD pipeline: build, parallel tests, staging deploy, production promotion, as a single traceable execution where every failure halts the line.

## From Git Commit to Production in One Pipeline

A developer pushes a commit to the main branch. The CI/CD pipeline must build the project from that commit SHA, run unit tests and integration tests in parallel (cutting test time in half), deploy to staging if tests pass, run smoke tests against staging, and deploy to production if staging is healthy. Each step depends on the previous one's success, and any failure should halt the pipeline with clear reporting.

The pipeline must be idempotent: re-running it with the same commit SHA should produce the same build. Tests should run in parallel to minimize pipeline time. The staging deployment must be verified before production deployment begins. And every pipeline run needs a complete audit trail. What was built, what tests ran, what was deployed, and when.

## The Solution

**You write the build and deploy logic. Conductor handles parallel test execution, staging-before-production gating, and full pipeline traceability.**

`BuildWorker` checks out the specified commit, compiles the code, and produces build artifacts. `FORK_JOIN` dispatches unit tests and integration tests to run in parallel. After `JOIN` collects both results, `DeployStagingWorker` deploys the build to the staging environment and runs smoke tests. `DeployProdWorker` promotes the verified build to production. Conductor runs tests in parallel, tracks the full pipeline execution, and records build-to-deploy traceability.

### What You Write: Workers

Four workers run the CI/CD pipeline. Building from a commit, running tests in parallel, deploying to staging, and promoting to production.

| Worker | Task | What It Does |
|---|---|---|
| **Build** | `cicd_build` | Builds the application from the specified repo, branch, and commit. |
| **DeployProd** | `cicd_deploy_prod` | Deploys the build to production environment. |
| **DeployStaging** | `cicd_deploy_staging` | Deploys the build to staging environment. |
| **SecurityScan** | `cicd_security_scan` | Runs security scan for the build. |

### The Workflow

```
cicd_build
 │
 ▼
FORK_JOIN
 ├── cicd_unit_test
 ├── cicd_integration_test
 └── cicd_security_scan
 │
 ▼
JOIN (wait for all branches)
cicd_deploy_staging
 │
 ▼
cicd_deploy_prod

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
