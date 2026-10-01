/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client.http;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.conductor.common.validation.ValidationError;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Setter;

@Data
public class ApiException extends RuntimeException {
    static boolean initPreferErrOverResponse() {
        try {
            final String propName = "conductor.client.exception.preferErrOverResponse";
            return Boolean.getBoolean(propName);
        } catch (SecurityException e) {
            return false;
        }
    }

    private final static boolean PREFER_ERR_OVER_RESPONSE = initPreferErrOverResponse();

    private static final int HTTP_PAYMENT_REQUIRED = 402;
    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_LOCKED = 423;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private int status;
    private String instance;
    private String code;
    @Setter private boolean retryable;
    @JsonIgnore @Setter private boolean definite;
    private List<ValidationError> validationErrors; //List of validation errors. Available when the status code is 400
    private Map<String, List<String>> responseHeaders;
    private String responseBody;

    public ApiException(String message) {
        super(message);
        this.responseBody = message;
    }

    public ApiException(int statusCode, String message) {
        super(message);
        this.status = statusCode;
        this.responseBody = message;
    }

    public ApiException(Throwable t) {
        super(t.getMessage(), t);
        this.responseBody = t.getMessage();
    }

    public ApiException(String message, Throwable t) {
        super(message, t);
        this.responseBody = message;
    }

    public ApiException(String message,
        int code,
        Map<String, List<String>> responseHeaders,
        String responseBody) {
        this(message, null, code, responseHeaders, responseBody);
    }

    public ApiException(String message,
        Throwable t,
        int code,
        Map<String, List<String>> responseHeaders) {
        this(message, t, code, responseHeaders, message);
    }

    public ApiException(String message,
                                    Throwable t,
                                    int code,
                                    Map<String, List<String>> responseHeaders,
                                    String responseBody) {
        super(message, t);
        this.code = String.valueOf(code);
        this.status = code;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public boolean isClientError() {
        return getStatus() > 399 && getStatus() < 499;
    }

    /**
     * Whether this error proves the request had no effect.
     *
     * <p>{@code true} means the server never applied the request, so retrying it is safe.
     *
     * <p>{@code false} means the outcome is unknown: the request may or may not have been applied.
     * It does not mean the request succeeded, and it does not mean retrying is unsafe — only that
     * a retry may duplicate the work. {@code false} is the default, because most transport
     * failures prove nothing.
     *
     * <p>A dropped connection is always indeterminate, including {@code ConnectException}. OkHttp
     * may retry a request on a fresh route after a pooled connection fails mid-send, so "failed to
     * connect" can follow a request the server already received.
     *
     * <p>This is not {@code isRetryable()}. That one says whether trying again is worth it; this
     * one says whether trying again can duplicate work. They are independent and often opposite: a
     * 503 is retryable and indeterminate at the same time.
     */
    public boolean isDefinite() {
        return definite;
    }

    /**
     * Whether an HTTP status proves the server rejected the request without applying it.
     *
     * <p>Most 4xx codes qualify. 402, 408, 409, 423 and 429 do not: Conductor can return these
     * after it has already written, so a caller that retried them could duplicate the work.
     *
     * <p>This classification reflects the current server's behaviour and may change as the server
     * changes; it is advisory, not a durable guarantee.
     */
    public static boolean definiteFor(int status) {
        return status >= 400
                && status < 500
                && status != HTTP_PAYMENT_REQUIRED
                && status != HTTP_REQUEST_TIMEOUT
                && status != HTTP_CONFLICT
                && status != HTTP_LOCKED
                && status != HTTP_TOO_MANY_REQUESTS;
    }

    /**
     * @return HTTP status code
     */
    public int getStatusCode() {
        return getStatus();
    }

    @Override
    public String getMessage() {
        if (PREFER_ERR_OVER_RESPONSE) {
            return StringUtils.isNotBlank(super.getMessage()) ? super.getMessage() : responseBody;
        }
        return StringUtils.isNotBlank(responseBody) ? responseBody : super.getMessage();
    }

    public void setError(String error) {
        this.responseBody = error;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName()).append(": ");

        if (getMessage() != null) {
            builder.append(getMessage());
        }

        if (status > 0) {
            builder.append(" {status=").append(status);
            if (this.code != null) {
                builder.append(", code='").append(code).append("'");
            }

            builder.append(", retryable: ").append(retryable);
        }

        builder.append(", definite: ").append(definite);

        if (this.instance != null) {
            builder.append(", instance: ").append(instance);
        }

        if (this.validationErrors != null) {
            builder.append(", validationErrors: ").append(validationErrors);
        }

        builder.append("}");
        return builder.toString();
    }

}
