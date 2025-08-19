package com.netflix.conductor.client.exception;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
}
