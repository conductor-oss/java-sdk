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
package org.conductoross.conductor.ai;

import io.orkes.conductor.client.ApiClient;

/** Test-only client construction (production code uses {@code ApiClient.builder()} directly). */
public final class TestClients {

    private TestClients() {}

    /** Build an unauthenticated client for a test server URL; appends {@code /api} if missing. */
    public static ApiClient forUrl(String url) {
        String s = url;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (!s.endsWith("/api")) s = s + "/api";
        return ApiClient.builder().basePath(s).build();
    }
}
