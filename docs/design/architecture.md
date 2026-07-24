# Architecture — Fix `SchedulerClient.pauseSchedule()` GET-then-PUT fallback (Issue #140)

## 1. Overview

`SchedulerResource` in the Java client supports two server flavors for the
schedule pause/resume endpoints:

- **Enterprise (Orkes)** servers accept `GET /scheduler/schedules/{name}/pause`.
- **OSS Conductor** servers only accept `PUT` on the same path.

To work against both without a configuration flag, the client optimistically
issues a `GET` first and, if the server reports that `GET` is not allowed for
that route, retries the identical call with `PUT`. This is implemented by
`SchedulerResource.executeGetThenPutOnMethodNotAllowed(...)`.

### The bug

The fallback only recognizes a **clean HTTP `405 Method Not Allowed`** as the
signal to retry with `PUT`. Conductor OSS `3.32.0-rc.10` (Spring MVC) does not
return `405` for this route — it returns **HTTP `500`** with the body
`Request method 'GET' is not supported`. Because the status is `500` and not
`405`, the fallback rethrows the exception instead of proceeding to the `PUT`,
so `pauseSchedule(...)` (and `resumeSchedule(...)`) fail against OSS servers.

```
com.netflix.conductor.client.exception.ConductorClientException:
    Request method 'GET' is not supported {status=500, retryable: false}
  at ...SchedulerResource.executeGetThenPutOnMethodNotAllowed(SchedulerResource.java:175)
  at ...SchedulerResource.pauseSchedule(SchedulerResource.java:125)
```

### The fix (scope)

Broaden the retry predicate in `executeGetThenPutOnMethodNotAllowed` so that it
treats the server's "method not supported" signal as a trigger to fall through
to the `PUT`, whether it arrives as a genuine `405` **or** as a `500` whose
message indicates the request method is not supported. All other failures
(auth, validation, unrelated application `500`s, network) keep their current
behavior and propagate unchanged.

This is a **minimal, focused** change confined to one private method plus its
tests. No public API, no new files, no dependency changes.

## 2. Tech stack

| Concern            | Choice                                                             |
|--------------------|--------------------------------------------------------------------|
| Language           | Java 21                                                            |
| Module             | `conductor-client`                                                 |
| HTTP client        | `com.netflix.conductor.client.http.ConductorClient` (OkHttp based) |
| Exceptions         | `com.netflix.conductor.client.exception.ConductorClientException`  |
| Tests              | JUnit 5 (`org.junit.jupiter`) + `okhttp3.mockwebserver.MockWebServer` |
| Build              | Gradle                                                             |

## 3. Module / file layout

Only **existing** files are touched. No new source files are created.

| File | Change | Responsibility |
|------|--------|----------------|
| `conductor-client/src/main/java/io/orkes/conductor/client/http/SchedulerResource.java` | **MODIFY** | Add the private helper `isMethodNotSupported(ConductorClientException)` and the two detection constants; update `executeGetThenPutOnMethodNotAllowed(...)` to use the helper. |
| `conductor-client/src/test/java/io/orkes/conductor/client/http/SchedulerResourceTest.java` | **MODIFY** | Add regression tests covering the `500 "Request method 'GET' is not supported"` fallthrough for both `pauseSchedule` and `resumeSchedule`, and confirm unrelated `500`s still propagate. |

No changes are required in
`agent-examples/.../Example99ScheduledAgent.java`; it is the reproduction, not
part of the fix.

## 4. Shared contracts (reused verbatim by every component)

These names, signatures, and constants are the single source of truth. The
supporting docs and the tests reuse them exactly.

### 4.1 Exception surface (existing — do not change)

`ConductorClientException extends ApiException`. Relevant accessors used by the
fix:

```java
int getStatus();          // HTTP status code, e.g. 405 or 500
String getMessage();      // resolves to responseBody when present (see ApiException.getMessage)
```

`ApiException.getMessage()` returns the `responseBody` when it is non-blank, so
for the OSS failure it yields the string `Request method 'GET' is not supported`.

### 4.2 Detection constants (new — private to `SchedulerResource`)

```java
private static final int HTTP_METHOD_NOT_ALLOWED = 405;

// Substring Spring MVC emits when a route rejects the HTTP verb.
// Matched case-insensitively against ConductorClientException.getMessage().
private static final String METHOD_NOT_SUPPORTED_MARKER = "request method 'get' is not supported";
```

### 4.3 Detection helper (new — private to `SchedulerResource`)

```java
/**
 * True when the server rejected the GET because the HTTP verb is not
 * supported for the route. Recognizes both a clean 405 and the OSS
 * Conductor behavior of returning 500 with a
 * "Request method 'GET' is not supported" body.
 */
private boolean isMethodNotSupported(ConductorClientException e);
```

Predicate (exact semantics):

```
message = e.getMessage()

isMethodNotSupported(e) ==
    e.getStatus() == HTTP_METHOD_NOT_ALLOWED
    || (message != null
        && message.toLowerCase(Locale.ROOT).contains(METHOD_NOT_SUPPORTED_MARKER))
```

`java.util.Locale` is imported for the case-insensitive comparison.

### 4.4 Fallback method (existing signature — body updated)

```java
private void executeGetThenPutOnMethodNotAllowed(
        ConductorClientRequest getRequest, ConductorClientRequest putRequest);
```

New body:

```java
try {
    client.execute(getRequest);
} catch (ConductorClientException e) {
    if (!isMethodNotSupported(e)) {
        throw e;
    }
    client.execute(putRequest);
}
```

### 4.5 Behavior contract

| GET response from server                                     | Client action                     |
|--------------------------------------------------------------|-----------------------------------|
| `2xx` (e.g. `204`)                                          | Succeed. No `PUT`.                |
| `405 Method Not Allowed`                                    | Retry with `PUT`.                 |
| `500` with body containing `Request method 'GET' is not supported` | Retry with `PUT`.          |
| Any other failure (`401`, `403`, `400`, unrelated `500`, IO) | Propagate the original exception. |

The `PUT` retry itself is **not** wrapped: if the `PUT` fails, that exception
propagates as-is (unchanged from current behavior).

Both `pauseSchedule(String, String)` and `resumeSchedule(String)` route through
`executeGetThenPutOnMethodNotAllowed`, so both benefit from the fix identically.

## 5. Non-goals

- No change to the public `SchedulerClient` / `OrkesSchedulerClient` API.
- No configuration flag to force GET vs PUT (the auto-fallback stays).
- No change to any other `SchedulerResource` endpoint or to `ConductorClient`.
- No change to how exceptions are constructed or to `ApiException`.
