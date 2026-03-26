# Safe DNS Record Changes with Conflict Validation

Changing a DNS record without checking for conflicts can silently break other services that
depend on the same domain. This workflow plans the change, validates it against existing
records, applies it to Route53, and confirms propagation -- so a typo in a CNAME does not
take down production.

## Workflow

```
domain, recordType, target
           |
           v
+--------------+     +----------------+     +--------------+     +----------------+
| dns_plan     | --> | dns_validate   | --> | dns_apply    | --> | dns_verify     |
+--------------+     +----------------+     +--------------+     +----------------+
  PLAN-1345            no conflicts          records updated      propagation
  success=true         detected              in Route53           confirmed
```

## Workers

**PlanWorker** -- Takes `domain`, `recordType`, and `target` inputs. Plans the DNS change
and returns `planId: "PLAN-1345"`, `success: true`.

**ValidateWorker** -- Checks the plan for conflicts against existing DNS records. No
conflicts detected. Returns `validate: true`.

**ApplyWorker** -- Applies the DNS record changes to Route53. Returns `apply: true`.

**VerifyWorker** -- Confirms DNS propagation is complete across all nameservers. Returns
`verify: true`.

## Tests

2 unit tests cover the DNS management pipeline. The workflow timeout is 600 seconds.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
