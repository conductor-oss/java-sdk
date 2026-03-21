# Certificate Rotation in Java with Conductor

It's 2 AM on a Saturday. Your wildcard TLS cert expired eleven minutes ago. Every service behind the load balancer is failing TLS handshakes, browsers are showing "Your connection is not private" to every customer, and service-to-service calls across your mesh are throwing `SSLHandshakeException`. The cert was set to expire "sometime in March" and the calendar reminder went to an engineer who left the company in January. Now someone needs to generate a new cert, deploy it to four load balancers and six reverse proxies, and verify handshakes, all while the entire platform is down. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to automate the full certificate rotation lifecycle, discover expiring certs, generate replacements, deploy to infrastructure, and verify the handshake, before anyone's pager goes off.

## Before the Certificate Expires

A TLS certificate is 15 days from expiration. Someone needs to notice, generate a replacement, deploy it to every load balancer and reverse proxy that serves the domain, and verify the new cert actually works, all without causing a service interruption. Miss this window and your customers see browser warnings or, worse, a hard outage.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the certificate lifecycle logic. Conductor handles discovery-to-verification sequencing, retries, and rotation audit trails.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the certificate lifecycle. Discovering expiring certs, generating replacements, deploying to load balancers, and verifying the TLS handshake.

| Worker | Task | What It Does |
|---|---|---|
| `DiscoverWorker` | `cr_discover` | Scans the given domain to find a certificate expiring within 15 days and returns a discovery ID |
| `GenerateWorker` | `cr_generate` | Issues a new certificate from the certificate authority for the expiring domain |
| `DeployWorker` | `cr_deploy` | Deploys the newly generated certificate to load balancers and reverse proxies |
| `VerifyWorker` | `cr_verify` | Performs a TLS handshake against the domain to confirm the new certificate is active and valid |

### The Workflow

```
cr_discover
 |
 v
cr_generate
 |
 v
cr_deploy
 |
 v
cr_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
