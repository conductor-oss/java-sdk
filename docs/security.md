# Security and secrets

**Audience:** production SDK and agent owners.  
**Security note:** workflow input, task input, execution output, and logs may be visible to operators. Do not treat them as secret stores.

## Credentials

Store server credentials in environment variables or a workload secret manager. Use `CONDUCTOR_AUTH_KEY` and `CONDUCTOR_AUTH_SECRET` where required. Restrict each deployment to the smallest server permissions and rotate credentials without writing their values to source control.

## Workflow and worker data

- Pass references to blobs rather than large or sensitive payloads.
- Redact personal data in logs and metrics.
- Use application-owned idempotency keys for external writes.
- Validate input at the workflow boundary before invoking tools or workers.

## Agent tools

Treat every tool as an API exposed to a model. Define narrow input schemas, authorize in the worker, validate arguments again at execution, and keep sensitive credentials server-side. Provider credentials must be available to the Conductor server for server-executed agent/model work. See [agent tools](agents/concepts/tools.md) and the [credential delivery contract](../design/secret-injection-contract.md).

## OSS and Orkes

Both need secret handling outside workflow input. Orkes-specific secret and integration capabilities are server features; confirm their availability with your tenant and use the platform's secret references rather than copying values into definitions.

Next: [connection and authentication](connection-authentication.md), [reliability](reliability.md), and [observability](observability.md).
