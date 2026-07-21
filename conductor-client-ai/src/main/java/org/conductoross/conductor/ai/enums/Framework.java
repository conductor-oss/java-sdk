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
package org.conductoross.conductor.ai.enums;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Framework identifiers for agents backed by a third-party agent SDK.
 *
 * <p>The server routes each framework through a matching {@code AgentConfigNormalizer}
 * before compilation. Every value here corresponds to a {@code frameworkId()} in the
 * server's normalizer registry. Native Conductor agents have no framework — their
 * config is sent as {@code agentConfig}.
 *
 * <p>Known server normalizers and their IDs:
 * <ul>
 *   <li>{@code OpenAINormalizer}       → {@link #OPENAI}</li>
 *   <li>{@code GoogleADKNormalizer}    → {@link #GOOGLE_ADK}</li>
 *   <li>{@code LangChainNormalizer}    → {@link #LANGCHAIN}</li>
 *   <li>{@code LangGraphNormalizer}    → {@link #LANGGRAPH}</li>
 *   <li>{@code SkillNormalizer}        → {@link #SKILL}</li>
 *   <li>{@code VercelAINormalizer}     → {@link #VERCEL_AI}</li>
 *   <li>{@code ClaudeAgentSdkNormalizer} → {@link #CLAUDE_AGENT_SDK}</li>
 * </ul>
 *
 * @see org.conductoross.conductor.ai.frameworks.OpenAIAgent
 * @see org.conductoross.conductor.ai.frameworks.AdkBridge
 * @see org.conductoross.conductor.ai.frameworks.LangChainBridge
 * @see org.conductoross.conductor.ai.skill.Skill
 */
public enum Framework {
    OPENAI("openai"),
    GOOGLE_ADK("google_adk"),
    LANGCHAIN("langchain"),
    LANGGRAPH("langgraph"),
    SKILL("skill"),
    VERCEL_AI("vercel_ai"),
    CLAUDE_AGENT_SDK("claude_agent_sdk");

    private final String wireValue;

    Framework(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The JSON/wire string value sent to and expected by the server. */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * Return the {@code Framework} matching {@code value}, or empty if
     * {@code value} is null, blank, or not a recognised framework identifier.
     */
    @JsonCreator
    public static Optional<Framework> of(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        for (Framework f : values()) {
            if (f.wireValue.equals(value)) return Optional.of(f);
        }
        return Optional.empty();
    }
}
