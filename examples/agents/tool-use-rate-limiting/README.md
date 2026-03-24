# Rate-Limited Tool Calls: Check Quota, SWITCH to Execute or Queue

Before calling, check the rate limit (`quotaUsed` vs `quotaLimit`). A SWITCH routes: if `"allowed"`, execute the tool directly (e.g., translation). If throttled (default), queue the request (`queueId: "q-fixed-001"`, `queuePosition`) and execute later via delayed execution.

## Workflow

```
toolName, toolArgs
  -> rl_check_rate_limit -> SWITCH(allowed: rl_execute_tool, default: rl_queue_request -> rl_delayed_execute)
```

## Tests

32 tests cover rate limit checking, direct execution, and queued delayed execution.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
