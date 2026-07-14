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
package org.conductoross.conductor.ai.examples;

/**
 * Shared settings for all examples. Reads from environment variables.
 *
 * <p>Set these before running examples:
 * <pre>
 * export AGENTSPAN_SERVER_URL=http://localhost:6767/api
 * export AGENTSPAN_LLM_MODEL=openai/gpt-4o
 * export AGENTSPAN_AUTH_KEY=your-key       # optional
 * export AGENTSPAN_AUTH_SECRET=your-secret # optional
 * </pre>
 */
public class Settings {
    private static final java.util.Map<String, String> ENV = System.getenv();

    public static final String SERVER_URL =
        ENV.getOrDefault("AGENTSPAN_SERVER_URL", "http://localhost:6767/api");

    public static final String LLM_MODEL =
        ENV.getOrDefault("AGENTSPAN_LLM_MODEL", "openai/gpt-4o");

    public static final String SECONDARY_LLM_MODEL =
        ENV.getOrDefault("AGENT_SECONDARY_LLM_MODEL", "anthropic/claude-sonnet-4-6");

    public static final String AUTH_KEY =
        ENV.get("AGENTSPAN_AUTH_KEY");

    public static final String AUTH_SECRET =
        ENV.get("AGENTSPAN_AUTH_SECRET");

    private Settings() {}
}
