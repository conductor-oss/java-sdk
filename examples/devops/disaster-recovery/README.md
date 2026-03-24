# Disaster Recovery Failover from us-east-1 to us-west-2

Your primary region just went down. The database needs to be promoted in the DR region,
DNS records need to point traffic to the backup, and someone needs to verify the whole
thing actually works -- all while the clock is ticking on your RTO. This workflow detects
the failure, fails over the database, updates DNS, and verifies recovery.

## Workflow

```
primaryRegion, drRegion
         |
         v
+--------------+     +------------------+     +------------------+     +--------------+
| dr_detect    | --> | dr_failover_db   | --> | dr_update_dns    | --> | dr_verify    |
+--------------+     +------------------+     +------------------+     +--------------+
  us-east-1 failure    DB promoted in          DNS records updated     recovered=true
  confirmed            us-west-2                                       RTO: 8 minutes
```

## Workers

**DetectWorker** -- Confirms the primary region (us-east-1) has failed. Returns
`failed: true`.

**FailoverDbWorker** -- Promotes the standby database in the DR region (us-west-2). Returns
`failedOver: true`.

**UpdateDnsWorker** -- Points DNS records to the DR region. Returns `updated: true`.

**VerifyWorker** -- Confirms the DR region is healthy. Returns `recovered: true` and
`rtoMinutes: 8`.

## Tests

2 unit tests cover the disaster recovery pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
