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

import java.util.List;
import java.util.Map;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientResponse;

import com.fasterxml.jackson.core.type.TypeReference;

/** Raw JSON requests, used where the client model classes would drop fields. */
final class Http {

    private Http() {}

    static <T> T post(ConductorClient client, String path, Object body, Class<T> type) {
        ConductorClientRequest req =
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.POST).path(path).body(body).build();
        ConductorClientResponse<T> resp = client.execute(req, ref(type));
        return resp == null ? null : resp.getData();
    }

    static void put(ConductorClient client, String path, Object body) {
        ConductorClientRequest req =
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.PUT).path(path).body(body).build();
        client.execute(req);
    }

    static Map<String, Object> getMap(ConductorClient client, String path, Map<String, String> query) {
        ConductorClientRequest.Builder b =
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path(path);
        if (query != null) {
            query.forEach(b::addQueryParam);
        }
        ConductorClientResponse<Map<String, Object>> resp = client.execute(b.build(), new TypeReference<>() {});
        return resp == null ? null : resp.getData();
    }

    static List<Map<String, Object>> getList(ConductorClient client, String path) {
        ConductorClientRequest req =
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path(path).build();
        ConductorClientResponse<List<Map<String, Object>>> resp = client.execute(req, new TypeReference<>() {});
        return resp == null ? null : resp.getData();
    }

    private static <T> TypeReference<T> ref(Class<T> type) {
        return new TypeReference<>() {
            @Override
            public java.lang.reflect.Type getType() {
                return type;
            }
        };
    }
}
