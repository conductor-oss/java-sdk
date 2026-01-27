/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.common.run.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;

/**
 * Typed wrapper for HTTP tasks providing convenient access to HTTP request/response properties.
 *
 * <p>Supports two input formats:
 * <ul>
 *   <li><b>Nested format</b>: HTTP properties inside an {@code http_request} object</li>
 *   <li><b>Flat format</b>: HTTP properties directly in {@code inputData}</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myHttpTask");
 * HttpTask http = new HttpTask(task);
 *
 * String url = http.getUri();
 * String method = http.getMethod();
 * Integer statusCode = http.getStatusCode();
 * }</pre>
 */
public class HttpTask extends TypedTask {

    public static final String HTTP_REQUEST_INPUT = "http_request";
    public static final String URI_KEY = "uri";
    public static final String METHOD_KEY = "method";
    public static final String HEADERS_KEY = "headers";
    public static final String BODY_KEY = "body";
    public static final String CONNECTION_TIMEOUT_KEY = "connectionTimeOut";
    public static final String READ_TIMEOUT_KEY = "readTimeOut";
    public static final String CONTENT_TYPE_KEY = "contentType";
    public static final String ACCEPT_KEY = "accept";
    public static final String ENCODE_KEY = "encode";
    public static final String ASYNC_COMPLETE_KEY = "asyncComplete";

    public static final String RESPONSE_OUTPUT = "response";
    public static final String STATUS_CODE_OUTPUT = "statusCode";
    public static final String HEADERS_OUTPUT = "headers";
    public static final String BODY_OUTPUT = "body";
    public static final String REASON_PHRASE_OUTPUT = "reasonPhrase";

    /**
     * Creates an HttpTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not an HTTP task
     */
    public HttpTask(Task task) {
        super(task, TaskType.TASK_TYPE_HTTP);
    }

    /**
     * Checks if the given task is an HTTP task.
     */
    public static boolean isHttpTask(Task task) {
        return task != null && TaskType.TASK_TYPE_HTTP.equals(task.getTaskType());
    }

    /**
     * Returns true if the input uses the nested http_request format.
     */
    public boolean isNestedFormat() {
        return task.getInputData().get(HTTP_REQUEST_INPUT) instanceof Map;
    }

    // ----- Request Properties -----

    /**
     * Returns the HTTP request input as a map (for nested format).
     * For flat format, returns an empty map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHttpRequest() {
        Object request = task.getInputData().get(HTTP_REQUEST_INPUT);
        if (request instanceof Map) {
            return (Map<String, Object>) request;
        }
        return Collections.emptyMap();
    }

    /**
     * Gets a request property, checking nested format first, then flat format.
     */
    private Object getRequestProperty(String key) {
        // First check nested http_request
        Map<String, Object> httpRequest = getHttpRequest();
        if (!httpRequest.isEmpty()) {
            Object value = httpRequest.get(key);
            if (value != null) {
                return value;
            }
        }
        // Fall back to flat format
        return task.getInputData().get(key);
    }

    /**
     * Returns the request URI, or null if not set.
     */
    public String getUri() {
        Object uri = getRequestProperty(URI_KEY);
        return uri != null ? uri.toString() : null;
    }

    /**
     * Returns the HTTP method (GET, POST, PUT, DELETE, etc.), or null if not set.
     */
    public String getMethod() {
        Object method = getRequestProperty(METHOD_KEY);
        return method != null ? method.toString().toUpperCase() : null;
    }

    /**
     * Returns the request headers.
     * For flat format, builds headers from contentType and accept if present.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRequestHeaders() {
        Object headers = getRequestProperty(HEADERS_KEY);
        if (headers instanceof Map) {
            return (Map<String, Object>) headers;
        }

        // For flat format, build headers from contentType and accept
        Map<String, Object> builtHeaders = new HashMap<>();
        String contentType = getContentType();
        String accept = getAccept();
        if (contentType != null) {
            builtHeaders.put("Content-Type", contentType);
        }
        if (accept != null) {
            builtHeaders.put("Accept", accept);
        }
        return builtHeaders.isEmpty() ? Collections.emptyMap() : builtHeaders;
    }

    /**
     * Returns the request body, or null if not set.
     */
    public Object getRequestBody() {
        return getRequestProperty(BODY_KEY);
    }

    /**
     * Returns the connection timeout in milliseconds, or null if not set.
     */
    public Integer getConnectionTimeout() {
        Object timeout = getRequestProperty(CONNECTION_TIMEOUT_KEY);
        if (timeout instanceof Number) {
            return ((Number) timeout).intValue();
        }
        return null;
    }

    /**
     * Returns the read timeout in milliseconds, or null if not set.
     */
    public Integer getReadTimeout() {
        Object timeout = getRequestProperty(READ_TIMEOUT_KEY);
        if (timeout instanceof Number) {
            return ((Number) timeout).intValue();
        }
        return null;
    }

    /**
     * Returns the content type, or null if not set.
     */
    @SuppressWarnings("unchecked")
    public String getContentType() {
        Object contentType = task.getInputData().get(CONTENT_TYPE_KEY);
        if (contentType != null) {
            return contentType.toString();
        }
        // Also check nested headers
        Object headers = getHttpRequest().get(HEADERS_KEY);
        if (headers instanceof Map) {
            Map<String, Object> headersMap = (Map<String, Object>) headers;
            Object ct = headersMap.get("Content-Type");
            if (ct != null) {
                return ct.toString();
            }
        }
        return null;
    }

    /**
     * Returns the accept header value, or null if not set.
     */
    @SuppressWarnings("unchecked")
    public String getAccept() {
        Object accept = task.getInputData().get(ACCEPT_KEY);
        if (accept != null) {
            return accept.toString();
        }
        // Also check nested headers
        Object headers = getHttpRequest().get(HEADERS_KEY);
        if (headers instanceof Map) {
            Map<String, Object> headersMap = (Map<String, Object>) headers;
            Object a = headersMap.get("Accept");
            if (a != null) {
                return a.toString();
            }
        }
        return null;
    }

    /**
     * Returns whether URL encoding is enabled, or null if not set.
     */
    public Boolean isEncode() {
        return getInputBoolean(ENCODE_KEY);
    }

    /**
     * Returns whether async complete is enabled, or null if not set.
     */
    public Boolean isAsyncComplete() {
        return getInputBoolean(ASYNC_COMPLETE_KEY);
    }

    // ----- Response Properties -----

    /**
     * Returns the HTTP response as a map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResponse() {
        Object response = task.getOutputData().get(RESPONSE_OUTPUT);
        if (response instanceof Map) {
            return (Map<String, Object>) response;
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the HTTP status code, or null if not available.
     */
    public Integer getStatusCode() {
        // Check both output data directly and nested in response
        Object statusCode = task.getOutputData().get(STATUS_CODE_OUTPUT);
        if (statusCode == null) {
            statusCode = getResponse().get(STATUS_CODE_OUTPUT);
        }
        if (statusCode instanceof Number) {
            return ((Number) statusCode).intValue();
        }
        return null;
    }

    /**
     * Returns the response headers.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResponseHeaders() {
        Object headers = task.getOutputData().get(HEADERS_OUTPUT);
        if (headers == null) {
            headers = getResponse().get(HEADERS_OUTPUT);
        }
        if (headers instanceof Map) {
            return (Map<String, Object>) headers;
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the response body, or null if not available.
     */
    public Object getResponseBody() {
        Object body = task.getOutputData().get(BODY_OUTPUT);
        if (body == null) {
            body = getResponse().get(BODY_OUTPUT);
        }
        return body;
    }

    /**
     * Returns the reason phrase (e.g., "OK", "Not Found"), or null if not available.
     */
    public String getReasonPhrase() {
        Object reason = task.getOutputData().get(REASON_PHRASE_OUTPUT);
        if (reason == null) {
            reason = getResponse().get(REASON_PHRASE_OUTPUT);
        }
        return reason != null ? reason.toString() : null;
    }

    /**
     * Returns true if the HTTP request was successful (2xx status code).
     */
    public boolean isSuccessful() {
        Integer code = getStatusCode();
        return code != null && code >= 200 && code < 300;
    }
}
