# Testing — Scheduler pause/resume GET-then-PUT fallback (Issue #140)

Test plan for the fix in [`architecture.md`](./architecture.md), verifying the
decision matrix in [`data-model.md`](./data-model.md). All names and rules are
reused verbatim from architecture §3.

## 1. Scope

- **Unit tests only.** The change is a pure control-flow adjustment inside
  `SchedulerResource`; no server, network, or persistence is required.
- Target file:
  `conductor-client/src/test/java/io/orkes/conductor/client/http/SchedulerResourceTest.java`
- Framework: JUnit 5 (Jupiter) + Mockito, matching existing `conductor-client`
  tests.

## 2. Test double strategy

Mock `com.netflix.conductor.client.http.ConductorClient` and inject it into
`new SchedulerResource(mockClient)`. Because both `GET` and `PUT` go through the
`void` overload `client.execute(ConductorClientRequest)`, distinguish the two
attempts by matching on the request `Method` with an `ArgumentMatcher` / captor.

Helper to build the thrown error, reused across cases:

```java
private static ConductorClientException clientError(int status, String message) {
    // ConductorClientException(int statusCode, String message)
    return new ConductorClientException(status, message);
}

// Matches a request whose method equals the given Method.
private static ConductorClientRequest req(Method method) {
    return argThat(r -> r != null && r.getMethod() == method);
}
```

## 3. Test matrix

Each row is one `@Test`. "GET" / "PUT" refer to `client.execute(...)` invoked with
that method on `/scheduler/schedules/{name}/pause` (or `/resume`).

| # | Test name | GET stub | Expected PUT? | Expected result |
|---|-----------|----------|---------------|-----------------|
| 1 | `pauseSchedule_getSucceeds_noPut` | returns normally (Enterprise) | **no** | no exception; `execute(GET)` once, `execute(PUT)` never |
| 2 | `pauseSchedule_405_fallsThroughToPut` | throws `clientError(405, "Method Not Allowed")` | **yes** | PUT executed once; no exception |
| 3 | `pauseSchedule_500MethodNotSupported_fallsThroughToPut` | throws `clientError(500, "Request method 'GET' is not supported")` | **yes** | PUT executed once; no exception — **the issue #140 case** |
| 4 | `pauseSchedule_500MethodNotSupported_caseInsensitive` | throws `clientError(500, "REQUEST METHOD 'GET' IS NOT SUPPORTED")` | **yes** | PUT executed once; no exception |
| 5 | `pauseSchedule_500Generic_rethrows` | throws `clientError(500, "NullPointerException")` | **no** | same exception rethrown (status 500) |
| 6 | `pauseSchedule_500NullMessage_rethrows` | throws `clientError(500, null)` | **no** | same exception rethrown |
| 7 | `pauseSchedule_401_rethrows` | throws `clientError(401, "Unauthorized")` | **no** | same exception rethrown (auth preserved) |
| 8 | `pauseSchedule_403_rethrows` | throws `clientError(403, "Forbidden")` | **no** | same exception rethrown |
| 9 | `pauseSchedule_400_rethrows` | throws `clientError(400, "bad request")` | **no** | same exception rethrown (validation preserved) |
| 10 | `pauseSchedule_putFails_propagates` | GET throws `clientError(405, ...)`, PUT throws `clientError(409, "conflict")` | yes (fails) | PUT's exception propagates unchanged |
| 11 | `resumeSchedule_500MethodNotSupported_fallsThroughToPut` | throws `clientError(500, "Request method 'GET' is not supported")` | **yes** | PUT executed once; no exception (same helper, resume path) |
| 12 | `pauseSchedule_withReason_reasonOnBothRequests` | throws `clientError(500, "Request method 'GET' is not supported")` | **yes** | captured GET and PUT both carry `reason` query param |

## 4. Representative test bodies

Issue #140 core case (row 3):

```java
@Test
void pauseSchedule_500MethodNotSupported_fallsThroughToPut() {
    doThrow(clientError(500, "Request method 'GET' is not supported"))
            .when(client).execute(req(Method.GET));
    // PUT succeeds (no stubbing needed for a void success)

    schedulerResource.pauseSchedule("eng_digest_99", "maintenance");

    verify(client).execute(req(Method.GET));
    verify(client).execute(req(Method.PUT));
    verifyNoMoreInteractions(client);
}
```

Negative case (row 5):

```java
@Test
void pauseSchedule_500Generic_rethrows() {
    doThrow(clientError(500, "NullPointerException"))
            .when(client).execute(req(Method.GET));

    ConductorClientException ex = assertThrows(
            ConductorClientException.class,
            () -> schedulerResource.pauseSchedule("eng_digest_99"));

    assertEquals(500, ex.getStatus());
    verify(client).execute(req(Method.GET));
    verify(client, never()).execute(req(Method.PUT));
}
```

## 5. Acceptance criteria

- All rows in §3 pass.
- No production change beyond `SchedulerResource.executeGetThenPutOnMethodNotAllowed`
  and its new private helper `isMethodNotAllowed` (architecture §3.3).
- `pauseSchedule`/`resumeSchedule` public signatures unchanged.
- Manual end-to-end check (out of CI): running
  `Example99ScheduledAgent` against conductor-oss `3.32.0-rc.10` completes the
  full lifecycle deploy → create → list → **pause** → resume → preview → delete
  without the `Request method 'GET' is not supported` failure.
