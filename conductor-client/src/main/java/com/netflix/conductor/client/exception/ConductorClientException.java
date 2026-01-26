/*
 * Copyright 2026 Conductor Authors.
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

import io.orkes.conductor.client.http.ApiException;

public class ConductorClientException extends ApiException {

    public ConductorClientException(String message) {
        super(message);
    }

    public ConductorClientException(String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        super(message, code, responseHeaders, responseBody);
    }

    public ConductorClientException(String message, Throwable t) {
        super(message, t);
    }

    public ConductorClientException(String message, Throwable t, int code, Map<String, List<String>> responseHeaders) {
        super(message, t, code, responseHeaders);
    }

    public ConductorClientException(String message, Throwable t, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        super(message, t, code, responseHeaders, responseBody);
    }

    public ConductorClientException(int statusCode, String message) {
        super(statusCode, message);
    }

    public ConductorClientException(Throwable t) {
        super(t);
    }
}
