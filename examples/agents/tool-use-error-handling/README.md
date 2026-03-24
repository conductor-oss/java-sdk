# Primary Tool with Fallback: SWITCH on Success/Failure

The primary tool deliberately returns `toolStatus: "failure"` with an error map. A SWITCH routes: on `"success"`, format the result (`source: "primary"`). On failure (default), try the fallback tool (returning geocoding coordinates `{37.7899, -122.4194}`) and format as `"Location: San Francisco, CA"` with `source: "fallback"`.

## Workflow

```
query, primaryTool, fallbackTool
  -> te_try_primary_tool -> SWITCH(success: te_format_success, default: te_try_fallback_tool -> te_format_fallback)
```

## Tests

33 tests cover primary success, primary failure with fallback, and formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
