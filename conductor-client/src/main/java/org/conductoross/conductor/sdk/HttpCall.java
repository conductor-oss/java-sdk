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
package org.conductoross.conductor.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Json;

import com.fasterxml.jackson.databind.JsonNode;

/** An HTTP call executed by the server: no worker, no client library in workflow code. */
public final class HttpCall {

    private final Command command;
    private final Map<String, Object> request = new LinkedHashMap<>();
    private final Map<String, Object> headers = new LinkedHashMap<>();

    HttpCall(String ref) {
        command = new Command(Command.Kind.HTTP, ref, ref);
        command.explicitRef = ref;
        command.httpBody = true;
        request.put("method", "GET");
        request.put("connectionTimeOut", 3000);
        request.put("readTimeOut", 10000);
        retries(3, java.time.Duration.ofSeconds(1));
    }

    /** Retries after a failure, with exponential backoff from the given delay; default 3 and 1s. */
    public HttpCall retries(int count, java.time.Duration delay) {
        command.fields.put(
                "taskDefinition",
                org.conductoross.conductor.sdk.internal.Entries.retryingDefinition(
                        command.name, count, Math.max(1, delay.toSeconds())));
        return this;
    }

    public HttpCall get(String uri) {
        return method("GET", uri);
    }

    public HttpCall post(String uri) {
        return method("POST", uri);
    }

    public HttpCall put(String uri) {
        return method("PUT", uri);
    }

    public HttpCall delete(String uri) {
        return method("DELETE", uri);
    }

    private HttpCall method(String method, String uri) {
        request.put("method", method);
        request.put("uri", uri);
        return this;
    }

    public HttpCall body(Object body) {
        request.put("body", Json.plain(body));
        return this;
    }

    public HttpCall header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    private Object run(Class<?> type) {
        if (!headers.isEmpty()) {
            request.put("headers", headers);
        }
        command.input.put("http_request", request);
        command.resultType = type;
        return Engine.current().call(command);
    }

    public <T> T as(Class<T> type) {
        return type.cast(run(type));
    }

    /** The response body as a JSON tree. */
    public JsonNode asJson() {
        Object body = run(Object.class);
        return Json.MAPPER.valueToTree(body);
    }
}
