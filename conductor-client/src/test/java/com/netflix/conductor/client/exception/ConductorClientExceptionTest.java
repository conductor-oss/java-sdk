/*
 * Copyright 2025 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.client.exception;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.http.ApiException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class ConductorClientExceptionTest {

    @Test
    void testMessageOnlyConstructor() {
        ConductorClientException ex = new ConductorClientException("hello");

        assertEquals("hello", ex.getMessage());
        assertEquals("hello", ex.getResponseBody());
        assertEquals(0, ex.getStatus());
        assertEquals(0, ex.getStatusCode());
        assertNull(ex.getCode());
        assertFalse(ex.isClientError());

        String toString = ex.toString();
        assertTrue(toString.contains("ConductorClientException"));
        assertTrue(toString.contains("hello"));
    }

    @Test
    void testStatusAndMessageConstructor() {
        ConductorClientException ex = new ConductorClientException(400, "bad request");

        assertEquals(400, ex.getStatus());
        assertEquals(400, ex.getStatusCode());
        assertEquals("bad request", ex.getMessage());
        assertEquals("bad request", ex.getResponseBody());
        assertNull(ex.getCode());
        assertTrue(ex.isClientError());

        String toString = ex.toString();
        assertTrue(toString.contains("status=400"));
        assertTrue(toString.contains("retryable"));
    }

    @Test
    void testThrowableOnlyConstructor() {
        Exception cause = new Exception("oops");
        ConductorClientException ex = new ConductorClientException(cause);

        assertEquals("oops", ex.getMessage());
        assertEquals("oops", ex.getResponseBody());
        assertSame(cause, ex.getCause());
        assertEquals(0, ex.getStatus());
        assertFalse(ex.isClientError());
    }

    @Test
    void testMessageAndThrowableConstructor() {
        Exception cause = new Exception("ignored");
        ConductorClientException ex = new ConductorClientException("wrap", cause);

        assertEquals("wrap", ex.getMessage());
        assertEquals("wrap", ex.getResponseBody());
        assertSame(cause, ex.getCause());
        assertEquals(0, ex.getStatus());
    }

    @Test
    void testFullConstructorWithResponseBody() {
        Map<String, List<String>> headers = Map.of("x", List.of("y"));
        ConductorClientException ex = new ConductorClientException("Conflict", null, 409, headers, "body");

        assertEquals("409", ex.getCode());
        assertEquals(409, ex.getStatus());
        assertEquals(headers, ex.getResponseHeaders());
        assertEquals("body", ex.getMessage()); // responseBody should override message in getMessage()
        assertEquals("body", ex.getResponseBody());
        assertTrue(ex.isClientError());

        // Update some optional fields and ensure toString reflects them
        ex.setRetryable(true);
        ex.setInstance("instance-1");

        String toString = ex.toString();
        assertTrue(toString.contains("status=409"));
        assertTrue(toString.contains("code='409'"));
        assertTrue(toString.contains("retryable: true"));
        assertTrue(toString.contains("instance: instance-1"));
    }

    @Test
    void testConstructorWithResponseHeadersDefaultBodyEqualsMessage() {
        Map<String, List<String>> headers = Map.of("h", List.of("v"));
        ConductorClientException ex = new ConductorClientException("message", new RuntimeException("cause"), 418, headers);

        assertEquals(418, ex.getStatus());
        assertEquals("418", ex.getCode());
        assertEquals(headers, ex.getResponseHeaders());
        assertEquals("message", ex.getResponseBody());
        assertEquals("message", ex.getMessage());
    }

    @Test
    void testGetMessageFallsBackWhenResponseBodyBlank() {
        Map<String, List<String>> headers = Map.of();
        ConductorClientException ex = new ConductorClientException("fallback", null, 429, headers, "");

        // Since responseBody is blank, getMessage() should return super.getMessage(), i.e., the constructor message
        assertEquals("fallback", ex.getMessage());
        assertEquals("", ex.getResponseBody());
        assertEquals(429, ex.getStatus());
        assertEquals("429", ex.getCode());
        assertTrue(ex.isClientError());
    }

    @Test
    void testIsClientErrorBoundaries() {
        assertFalse(new ConductorClientException(399, "").isClientError());
        assertTrue(new ConductorClientException(400, "").isClientError());
        assertTrue(new ConductorClientException(498, "").isClientError());
        assertFalse(new ConductorClientException(499, "").isClientError());
        assertFalse(new ConductorClientException(500, "").isClientError());
    }

    @Test
    @DisplayName("An error is indeterminate unless something proves otherwise")
    void isDefinite_byDefault_isFalse() {
        var e = new ConductorClientException("boom");
        assertFalse(e.isDefinite(), "the safe default is indeterminate");
    }

    @Test
    @DisplayName("The flag round-trips, so the getter cannot quietly become a constant")
    void setDefinite_thenIsDefinite_isTrue() {
        var e = new ConductorClientException("boom");
        e.setDefinite(true);
        assertTrue(e.isDefinite());
    }

    @Test
    @DisplayName("A plain 4xx means the server rejected the request without applying it")
    void definiteFor_clientErrors_isTrue() {
        assertTrue(ApiException.definiteFor(400));
        assertTrue(ApiException.definiteFor(401));
        assertTrue(ApiException.definiteFor(403));
        assertTrue(ApiException.definiteFor(404));
        assertTrue(ApiException.definiteFor(405));
        assertTrue(ApiException.definiteFor(415));
    }

    @Test
    @DisplayName("A 5xx may have applied the write before failing, so it stays indeterminate")
    void definiteFor_serverErrors_isFalse() {
        assertFalse(ApiException.definiteFor(500));
        assertFalse(ApiException.definiteFor(502));
        assertFalse(ApiException.definiteFor(503));
        assertFalse(ApiException.definiteFor(504));
    }

    @Test
    @DisplayName("Conductor can return these five 4xx codes after it has already written")
    void definiteFor_postWriteClientErrors_isFalse() {
        assertFalse(ApiException.definiteFor(402), "a definition can be written before replaceTags throws PAYMENT_REQUIRED");
        assertFalse(ApiException.definiteFor(408), "the server may have begun processing a partial request");
        assertFalse(ApiException.definiteFor(409), "FAIL_ON_RUNNING throws CONFLICT after createOnly, without removing the row");
        assertFalse(ApiException.definiteFor(423), "LOCK is returned on paths that invite a retry");
        assertFalse(ApiException.definiteFor(429), "RATE_LIMITED is thrown after createOnly");
    }

    @Test
    @DisplayName("No status means no response, which proves nothing")
    void definiteFor_noStatus_isFalse() {
        assertFalse(ApiException.definiteFor(0));
        assertFalse(ApiException.definiteFor(200));
    }

    @Test
    @DisplayName("toString renders a clean brace with no status, the Jepsen case the status>0 guard would hide definite in")
    void toString_withNoStatus_rendersDefiniteWithoutGarbage() {
        var e = new ConductorClientException("connection failed");
        e.setDefinite(false);

        assertEquals(0, e.getStatus(), "this is the no-response case the guard must not hide definite behind");
        assertEquals(
                "com.netflix.conductor.client.exception.ConductorClientException: connection failed {definite: false}",
                e.toString());
    }

    @Test
    @DisplayName("toString with a status keeps the existing status>0 shape, now followed by definite")
    void toString_withStatus_rendersStatusThenDefinite() {
        var e = new ConductorClientException(400, "bad request");
        e.setRetryable(true);
        e.setDefinite(true);

        assertEquals(
                "com.netflix.conductor.client.exception.ConductorClientException: bad request {status=400, retryable: true, definite: true}",
                e.toString());
    }

    @Test
    @DisplayName("A server error body cannot talk the client into claiming definiteness")
    void definite_isNotDeserializedFromTheResponseBody() throws Exception {
        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var e = mapper.readValue("{\"definite\":true,\"code\":\"X\"}", ConductorClientException.class);
        assertFalse(e.isDefinite(), "definiteness is the client's call, not the server's");
    }
}
