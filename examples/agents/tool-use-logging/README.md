# Tool Use with Full Audit Trail

Every tool call gets logged: request logging (generating a `requestId`), execution (e.g., sentiment analysis returning `emotions` map with scores), response logging, and an audit entry with `compliance` metadata.

## Workflow

```
toolName, toolArgs, userId, sessionId
  -> tl_log_request -> tl_execute_tool -> tl_log_response -> tl_create_audit_entry
```

## Tests

33 tests cover request logging, execution, response logging, and audit entry creation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
