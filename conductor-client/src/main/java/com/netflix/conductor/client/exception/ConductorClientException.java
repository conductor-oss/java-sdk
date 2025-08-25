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
package com.netflix.conductor.client.exception;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.conductor.common.validation.ValidationError;

import lombok.Data;
import lombok.Setter;

@Data
public class ConductorClientException extends RuntimeException {
    static boolean initPreferErrOverResponse() {
        try {
            final String propName = "conductor.client.exception.preferErrOverResponse";
            return Boolean.getBoolean(propName);
        } catch (SecurityException e) {
            return false;
        }
    }

    private final static boolean PREFER_ERR_OVER_RESPONSE = initPreferErrOverResponse();

    private int status;
    private String instance;
    private String code;
    @Setter
    private boolean retryable;
    private List<ValidationError> validationErrors; //List of validation errors. Available when the status code is 400
    private Map<String, List<String>> responseHeaders;
    private String responseBody;

    public ConductorClientException(String message) {
        super(message);
        this.responseBody = message;
    }

    public ConductorClientException(int statusCode, String message) {
        super(message);
        this.status = statusCode;
        this.responseBody = message;
    }

    public ConductorClientException(Throwable t) {
        super(t.getMessage(), t);
        this.responseBody = t.getMessage();
    }

    public ConductorClientException(String message, Throwable t) {
        super(message, t);
        this.responseBody = message;
    }

    public ConductorClientException(String message,
        int code,
        Map<String, List<String>> responseHeaders,
        String responseBody) {
        this(message, null, code, responseHeaders, responseBody);
    }

    public ConductorClientException(String message,
        Throwable t,
        int code,
        Map<String, List<String>> responseHeaders) {
        this(message, t, code, responseHeaders, message);
    }

    public ConductorClientException(String message,
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
