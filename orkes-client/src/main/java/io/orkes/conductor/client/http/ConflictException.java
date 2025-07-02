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

public class ConflictException extends OrkesClientException {
    private int code;
    private final Map<String, List<String>> responseHeaders;
    private String responseBody;
    private String message;

    public ConflictException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        super(message, throwable);
        super.setCode(String.valueOf(code));
        super.setStatus(code);
        this.code = code;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.message = message;
    }

    public ConflictException(String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(message, null, code, responseHeaders, responseBody);
        super.setCode(String.valueOf(code));
        super.setStatus(code);
        this.code = code;
        this.message = message;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return this.code;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return this.responseHeaders;
    }

    @Override
    public String getMessage() {
        int statusCode = this.getStatusCode();
        return statusCode + ":" + (StringUtils.isBlank(this.responseBody) ? super.getMessage() : this.responseBody);
    }

    @Override
    public String toString() {
        return this.responseBody;
    }

    public boolean isClientError() {
        return this.code > 399 && this.code < 499;
    }
}
