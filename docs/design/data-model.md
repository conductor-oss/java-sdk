# Data Model & Request Flow — Scheduler pause/resume fallback (Issue #140)

This document details the request/response data and the decision flow for the
GET-then-PUT fallback described in `architecture.md`. All names, constants, and
signatures are those defined in `architecture.md` §4.

## 1. Endpoints involved

| Operation | Method (Enterprise) | Method (OSS) | Path |
|-----------|---------------------|--------------|------|
| pause     | `GET`               | `PUT`        | `/scheduler/schedules/{name}/pause`  |
| resume    | `GET`               | `PUT`        | `/scheduler/schedules/{name}/resume` |

`pause` additionally carries an optional `reason` query parameter. Per the
existing behavior, `reason` is set on **both** the prepared `GET` and `PUT`
requests before the fallback runs.

## 2. Server response shapes

There is no request/response body model to add — the fix keys entirely off the
HTTP status and the error message already surfaced by
`ConductorClientException`.

### 2.1 Enterprise server (GET accepted)

```
HTTP/1.1 204 No Content
```

→ Client returns normally; no `PUT` is issued.

### 2.2 OSS server (GET rejected) — the case Issue #140 fixes

```
HTTP/1.1 500 Internal Server Error
Content-Type: text/plain

Request method 'GET' is not supported
```

The `ConductorClient` maps this to:

```
ConductorClientException {
    status       = 500
    responseBody = "Request method 'GET' is not supported"
    getMessage() -> "Request method 'GET' is not supported"
}
```

### 2.3 Older / spec-compliant server (GET rejected)

```
HTTP/1.1 405 Method Not Allowed
```

```
ConductorClientException { status = 405, ... }
```

## 3. Decision flow

```
pauseSchedule(name, reason) / resumeSchedule(name)
        |
        v
build getRequest (GET) + putRequest (PUT)   // reason applied to both for pause
        |
        v
executeGetThenPutOnMethodNotAllowed(getRequest, putRequest)
        |
        +-- client.execute(getRequest)
        |        |
        |        +-- success (2xx) --------------------> return
        |        |
        |        +-- throws ConductorClientException e
        |                 |
        |                 v
        |        isMethodNotSupported(e) ?
        |          status == 405                                   -> true
        |          OR getMessage() contains                        -> true
        |             "request method 'get' is not supported"
        |             (case-insensitive)
        |          else                                            -> false
        |                 |
        |     true -------+------- false
        |      |                     |
        |      v                     v
        |  client.execute(putRequest)   throw e   // propagate unchanged
        |      |
        |      +-- success -> return
        |      +-- failure -> propagate PUT exception (unchanged)
```

## 4. Decision matrix (authoritative)

Mirrors `architecture.md` §4.5. This is the exact truth table the
implementation and tests must satisfy.

| # | GET result                                         | `isMethodNotSupported` | PUT issued? | Outcome                     |
|---|----------------------------------------------------|------------------------|-------------|-----------------------------|
| 1 | `204`                                              | n/a                    | no          | success                     |
| 2 | `405`                                              | `true`                 | yes         | PUT outcome                 |
| 3 | `500` body `Request method 'GET' is not supported` | `true`                 | yes         | PUT outcome                 |
| 4 | `500` unrelated body (e.g. `boom`)                 | `false`                | no          | original exception rethrown |
| 5 | `401` / `403` / `400`                              | `false`                | no          | original exception rethrown |

Row 4 is the guardrail that keeps the fix minimal: only the specific
"method not supported" `500` falls through; every other `500` still fails fast.
This preserves the existing "do not retry non-405 failures" contract in spirit
(that test is updated to use an unrelated `500` body — see `testing.md`).

## 5. Why match on message, not just status

A blanket "retry any `500` with `PUT`" would mask genuine server errors and
double-fire mutating requests. Matching the Spring MVC marker string
`request method 'get' is not supported` (case-insensitive, via
`METHOD_NOT_SUPPORTED_MARKER`) restricts the fallthrough to exactly the
verb-rejection condition, keeping the behavior safe and targeted.
