# Distributing a Request Batch Across Instances with FORK_JOIN

A batch of requests arrives and needs to be processed across multiple service instances in
parallel to reduce total latency. This workflow uses FORK_JOIN to distribute partitions to
3 instances, each processing its share on a different host, then aggregates the results with
total processed count and average latency.

## Workflow

```
requestBatch
     |
     v
  FORK_JOIN: lb_call_instance (3 instances)
    instance-1: partition A on host-1
    instance-2: partition B on host-2
    instance-3: partition C on host-3
     |
     v
  JOIN
     |
     v
+-----------------------+
| lb_aggregate_results  |   totalProcessed, avgLatency
+-----------------------+
```

## Workers

**CallInstanceWorker** -- Processes a partition on a specific host. Returns a result map
with the `instanceId`, `partition`, and processing details.

**AggregateResultsWorker** -- Combines results from all instances. Returns `totalProcessed`,
`avgLatency`, and the aggregated result list.

## Tests

16 unit tests cover instance calls and result aggregation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
