# Tool Use Error Handling: Primary/Fallback with SWITCH on Status

An application depends on a geocoding API that occasionally returns 503 errors during peak traffic. Failing the entire workflow because of one flaky upstream service is unacceptable when an alternative provider exists. The system needs to try the primary tool, detect failure, automatically switch to a fallback, and still deliver a formatted result.

This workflow tries the primary tool, inspects the `toolStatus` field via a SWITCH task, and falls back to an alternative tool when the primary returns a failure.

## Pipeline Architecture

```
query, primaryTool, fallbackTool
         |
         v
  te_try_primary_tool    (toolStatus="failure", error={code:503, message})
         |
         v
  SWITCH on toolStatus
     |              |
  success        default (failure)
     |              |
     v              v
  te_format      te_try_fallback_tool
  _success          |
                    v
                 te_format_fallback
```

## Worker: TryPrimaryTool (`te_try_primary_tool`)

Deliberately simulates a failure by returning `toolStatus: "failure"` with an error map containing `code: 503`, `message: "Service temporarily unavailable"`, and the `tool` name. Sets `result: null` and records `attemptedAt: "2026-03-08T10:00:00Z"`. This lets the SWITCH task route to the fallback branch.

## Worker: FormatSuccess (`te_format_success`)

Activated on the `"success"` branch. Passes through the primary result with `source: "primary"` and `reliable: true`. In this demonstration, this branch is not reached because the primary always fails.

## Worker: TryFallbackTool (`te_try_fallback_tool`)

Receives the `primaryError` map and extracts the `message` as `fallbackReason`. Returns a successful geocoding result: `location: "San Francisco, CA"`, `coordinates: {lat: 37.7899, lng: -122.4194}`, `confidence: 0.97`, and `provider` set to the fallback tool name. Returns `toolStatus: "success"`.

## Worker: FormatFallback (`te_format_fallback`)

Formats the fallback result into a human-readable string: `"Location: San Francisco, CA (37.7899, -122.4194)"`. Sets `source: "fallback"`, `reliable: true`, and `fallbackUsed: true` so downstream consumers know the primary was bypassed.

## Tests

4 tests cover primary failure, fallback success, and both formatting paths.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
