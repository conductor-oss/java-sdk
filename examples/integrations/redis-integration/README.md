# Redis Integration

Orchestrates redis integration through a multi-stage Conductor workflow.

**Input:** `redisHost`, `cacheKey`, `cacheValue`, `channel` | **Timeout:** 60s

## Pipeline

```
red_connect
    │
red_get_set
    │
red_pub_sub
    │
red_cache_mgmt
```

## Workers

**CacheMgmtWorker** (`red_cache_mgmt`): Manages cache TTL and memory.

Reads `key`. Outputs `ttl`, `memoryUsage`, `encoding`.

**GetSetWorker** (`red_get_set`): Performs GET/SET operations.

Reads `key`, `value`. Outputs `setOk`, `retrievedValue`.

**PubSubWorker** (`red_pub_sub`): Publishes a message to a Redis channel.

Reads `channel`. Outputs `published`, `subscriberCount`, `channel`.

**RedisConnectWorker** (`red_connect`): Connects to a Redis instance.

```java
String connectionId = "redis-" + System.currentTimeMillis();
```

Reads `host`. Outputs `connectionId`, `connected`, `serverVersion`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
