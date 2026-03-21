# Redis Integration in Java Using Conductor

## Coordinating Redis Operations Across Connection, Cache, and Pub/Sub

Redis workflows often involve multiple operation types that share a connection: connecting to the server, writing and reading cache entries, managing TTL policies, and publishing messages to channels for real-time notifications. Each step depends on the connection established in the first step, and cache management needs to happen before publishing events that reference cached data.

Without orchestration, you would manage Redis connection IDs manually, chain Jedis or Lettuce calls, and handle connection lifecycle and error recovery yourself. Conductor sequences the operations and passes connection IDs and key references between workers automatically.

## The Solution

**You just write the Redis workers. Connection management, GET/SET operations, cache TTL configuration, and Pub/Sub messaging. Conductor handles connection lifecycle ordering, Redis operation retries, and connection ID routing between cache and messaging stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers exercise Redis operations: RedisConnectWorker establishes the connection, GetSetWorker performs key-value reads and writes, CacheMgmtWorker configures TTL and eviction policies, and PubSubWorker publishes messages to channels.

| Worker | Task | What It Does |
|---|---|---|
| **RedisConnectWorker** | `red_connect` | Connects to the Redis instance. establishes a connection to the specified host and returns the connectionId for use by subsequent operations |
| **GetSetWorker** | `red_get_set` | Performs GET/SET operations. writes the key-value pair and reads it back to confirm, returning the stored value |
| **CacheMgmtWorker** | `red_cache_mgmt` | Manages cache TTL and memory. sets expiration times on keys, checks memory usage, and applies eviction policies |
| **PubSubWorker** | `red_pub_sub` | Publishes a message to a Redis channel. sends the message to the specified Pub/Sub channel for real-time notification of subscribers |

the workflow orchestration and error handling stay the same.

### The Workflow

```
red_connect
 │
 ▼
red_get_set
 │
 ▼
red_pub_sub
 │
 ▼
red_cache_mgmt

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
