/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Builder for HTTP server-side tools.
 *
 * <p>HTTP tools are executed server-side by making HTTP requests. No local function is needed.
 *
 * <p>Example:
 * <pre>{@code
 * ToolDef weatherTool = HttpTool.builder()
 *     .name("get_weather")
 *     .description("Get the current weather for a city")
 *     .url("https://api.weather.com/current")
 *     .method("GET")
 *     .header("Authorization", "Bearer ${WEATHER_API_KEY}")
 *     .credentials("WEATHER_API_KEY")
 *     .build();
 * }</pre>
 */
public class HttpTool {

    private HttpTool() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description = "";
        private String url;
        private String method = "GET";
        private Map<String, String> headers = new HashMap<>();
        private List<String> accept = new ArrayList<>();
        private String contentType;
        private Map<String, Object> inputSchema;
        private List<String> credentials = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }

        public Builder accept(String... accept) {
            this.accept = new ArrayList<>(List.of(accept));
            return this;
        }

        public Builder accept(List<String> accept) {
            this.accept = new ArrayList<>(accept);
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder inputSchema(Map<String, Object> inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public Builder credentials(String... credentials) {
            for (String cred : credentials) {
                this.credentials.add(cred);
            }
            return this;
        }

        public Builder credentials(List<String> credentials) {
            this.credentials = new ArrayList<>(credentials);
            return this;
        }

        public ToolDef build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("HttpTool requires a name");
            }
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("HttpTool requires a URL");
            }

            Map<String, Object> config = new HashMap<>();
            config.put("url", url);
            config.put("method", method);
            config.put("headers", headers);
            if (!accept.isEmpty()) config.put("accept", accept);
            if (contentType != null) config.put("contentType", contentType);

            // Build a basic input schema if not provided
            Map<String, Object> schema = inputSchema;
            if (schema == null) {
                schema = new HashMap<>();
                schema.put("type", "object");
                schema.put("properties", new HashMap<>());
            }

            return new ToolDef.Builder()
                    .name(name)
                    .description(description)
                    .inputSchema(schema)
                    .toolType("http")
                    .config(config)
                    .credentials(credentials)
                    .build();
        }
    }
}
