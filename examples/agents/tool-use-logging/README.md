# Tool Use Logging: Request/Response Logs with Audit Trail

A regulated platform needs to track every tool invocation for compliance. Each API call must be logged before execution (who requested what), logged after execution (what came back and how long it took), and summarized into an immutable audit entry with GDPR compliance metadata. Without this, the team cannot answer "who called which tool, when, and what did it return?"

This workflow wraps tool execution in a four-stage observability pipeline: log the request, execute the tool, log the response, and create the audit entry.

## Pipeline Architecture

```
toolName, toolArgs, userId, sessionId
         |
         v
  tl_log_request         (requestId="req-fixed-abc123", timestamp, logEntry)
         |
         v
  tl_execute_tool        (sentiment result, executionTimeMs=187, toolStatus)
         |
         v
  tl_log_response        (timestamp, logEntry with direction="response")
         |
         v
  tl_create_audit_entry  (auditId, summary, compliance map)
```

## Worker: LogRequest (`tl_log_request`)

Records the incoming request with `toolName`, `userId`, `sessionId`, and a fixed `requestId: "req-fixed-abc123"`. The `logEntry` map includes all these fields plus `timestamp: "2026-03-08T10:00:00Z"` and `direction: "request"`. Returns `logged: true`.

## Worker: ExecuteTool (`tl_execute_tool`)

Performs sentiment analysis on the text from `toolArgs`. Returns a `sentimentResult` map with `sentiment: "positive"`, `confidence: 0.94`, `language: "en"`, `wordCount: 15`, and an `emotions` map containing six scores: `joy: 0.72`, `trust: 0.65`, `anticipation: 0.58`, `surprise: 0.12`, `anger: 0.02`, `sadness: 0.05`. Reports `executionTimeMs: 187` and `toolStatus: "success"`.

## Worker: LogResponse (`tl_log_response`)

Records the response with the correlated `requestId`, `toolName`, `toolStatus`, and `executionTimeMs`. The `logEntry` map mirrors the request format but with `direction: "response"` and `timestamp: "2026-03-08T10:00:01Z"` (one second after the request).

## Worker: CreateAuditEntry (`tl_create_audit_entry`)

Assembles a comprehensive audit entry with `auditId: "aud-fixed-001"`. The `summary` string includes the tool name, user ID, session ID, request ID, and execution time. The `compliance` map contains `gdprCompliant: true`, `dataRetentionDays: 90`, and `piiDetected: false`. The `metadata` map stores all timing and identity fields for downstream querying.

## Tests

4 tests cover request logging, tool execution, response logging, and audit entry creation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
