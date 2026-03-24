# Identity Provisioning

A new engineer joins the company. The system must create their identity in the directory, assign the senior-engineer role, provision access to GitHub, AWS, Slack, and Jira, and verify the user can access all provisioned systems.

## Workflow

```
ip_create_identity ──> ip_assign_roles ──> ip_provision_access ──> ip_verify_setup
```

Workflow `identity_provisioning_workflow` accepts `userId`, `department`, and `role`. Times out after `300` seconds.

## Workers

**CreateIdentityWorker** (`ip_create_identity`) -- creates a directory entry. Reports `"Created identity for jane.doe in engineering"`. Returns `create_identityId` = `"CREATE_IDENTITY-1358"`.

**AssignRolesWorker** (`ip_assign_roles`) -- assigns roles based on department. Reports `"Assigned role: senior-engineer"`. Returns `assign_roles` = `true`.

**ProvisionAccessWorker** (`ip_provision_access`) -- provisions access to systems. Reports `"Provisioned: GitHub, AWS, Slack, Jira"`. Returns `provision_access` = `true`.

**VerifySetupWorker** (`ip_verify_setup`) -- confirms all systems are accessible. Reports `"User can access all provisioned systems"`. Returns `verify_setup` = `true`.

## Workflow Output

The workflow produces `create_identityResult`, `verify_setupResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `identity_provisioning_workflow` defines 4 tasks with input parameters `userId`, `department`, `role` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end identity provisioning pipeline from creation through verification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
