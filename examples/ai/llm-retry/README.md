# Surviving LLM Rate Limits with Automatic Retries

LLM APIs fail transiently -- 429 Too Many Requests, 503 Service Unavailable -- and without retry logic, each failure crashes your pipeline. This workflow demonstrates Conductor's built-in retry mechanism: the worker deliberately fails the first two attempts (simulating rate limits), then succeeds on the third.

## Workflow

```
prompt, model
     │
     ▼
┌──────────────────┐
│ retry_llm_call   │  Fail twice, succeed on attempt 3
└────────┬─────────┘
         │  response, model, attempts
         ▼
┌──────────────────┐
│ retry_report     │  Report attempt count
└──────────────────┘
         │
         ▼
   response, model, attempts, summary
```

## Workers

**RetryLlmCallWorker** (`retry_llm_call`) -- Uses a static `ConcurrentHashMap<String, Integer>` called `attemptTracker` keyed by workflow ID. On each execution, calls `attemptTracker.merge(wfId, 1, Integer::sum)` to increment the count. If `attempt <= 2`, returns `FAILED` with `reasonForIncompletion: "429 Too Many Requests (attempt N)"` and `error: "rate_limited"`. On attempt 3+, when `CONDUCTOR_OPENAI_API_KEY` is set, calls `gpt-4o-mini` Chat Completions. In fallback mode, returns a fixed response. Conductor's retry configuration replays the task automatically.

**RetryReportWorker** (`retry_report`) -- Logs the number of attempts and returns `summary: "Retry succeeded"`.

## Tests

8 tests cover the attempt tracking, failure on early attempts, success after threshold, and report generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
