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

import io.orkes.conductor.client.OrkesClientException;

public class ApiException extends OrkesClientException {
    private int code;
    private Map<String, List<String>> responseHeaders;
    private String responseBody;

    public ApiException() {
        super("API Exception");
        this.code = 0;
        this.responseHeaders = null;
        this.responseBody = null;
    }

    public ApiException(Throwable throwable) {
        super(throwable.getMessage(), throwable);
        this.code = 0;
        this.responseHeaders = null;
        this.responseBody = null;
    }

    public ApiException(String message) {
        super(message);
        this.code = 0;
        this.responseHeaders = null;
        this.responseBody = null;
    }

    public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        super(message, throwable);
        this.code = 0;
        this.responseHeaders = null;
        this.responseBody = null;
        super.setCode(String.valueOf(code));
        super.setStatus(code);
        this.code = code;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public ApiException(String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(message, null, code, responseHeaders, responseBody);
        super.setCode(String.valueOf(code));
        super.setStatus(code);
    }

    public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders) {
        this(message, throwable, code, responseHeaders, null);
        super.setCode(String.valueOf(code));
        super.setStatus(code);
    }

    public ApiException(int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(null, null, code, responseHeaders, responseBody);
        super.setCode(String.valueOf(code));
        super.setStatus(code);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = 0;
        this.responseHeaders = null;
        this.responseBody = null;
        this.code = code;
        super.setCode(String.valueOf(code));
        super.setStatus(code);
    }

    public ApiException(int code, String message, Map<String, List<String>> responseHeaders, String responseBody) {
        this(code, message);
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        super.setCode(String.valueOf(code));
        super.setStatus(code);
    }

    public boolean isClientError() {
        return this.code > 399 && this.code < 499;
    }

    public int getStatusCode() {
        return this.code;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return this.responseHeaders;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    @Override
    public String getMessage() {
        int statusCode = this.getStatusCode();
        return statusCode + ":" + (StringUtils.isBlank(this.responseBody) ? super.getMessage() : this.responseBody);
    }
}
