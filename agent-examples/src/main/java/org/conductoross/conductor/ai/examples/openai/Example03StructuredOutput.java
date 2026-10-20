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
package org.conductoross.conductor.ai.examples.openai;

import java.util.List;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.frameworks.OpenAIAgent;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example OpenAi 03 — Structured Output
 *
 * <p>Java port of <code>sdk/python/examples/openai/03_structured_output.py</code>.
 *
 * <p>Demonstrates: forcing an OpenAI Agents SDK agent to return a typed
 * JSON object matching a Java record schema. The Python example uses a
 * Pydantic model; here we use a Java record and pass its simple name via
 * {@code .outputType(...)}.
 *
 * <p>Python parity gap: the Python example passes
 * {@code ModelSettings(temperature=0.3, max_tokens=1000)}. The current
 * {@link OpenAIAgent} builder does not expose model_settings, so we omit
 * those knobs — the rest of the agent shape is faithfully ported.
 *
 * <p>Expected JSON shape (matches {@link MovieList}):
 * <pre>{@code
 * {
 *   "recommendations": [
 *     {"title": "...", "year": 2014, "genre": "...", "reason": "..."}
 *   ],
 *   "theme": "..."
 * }
 * }</pre>
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini</li>
 * </ul>
 */
public class Example03StructuredOutput {

    /** Single movie recommendation — mirrors Python's MovieRecommendation pydantic model. */
    public record MovieRecommendation(String title, int year, String genre, String reason) {}

    /** Top-level recommendations payload — mirrors Python's MovieList pydantic model. */
    public record MovieList(List<MovieRecommendation> recommendations, String theme) {}

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = OpenAIAgent.builder()
                .name("movie_recommender")
                .instructions(
                        "You are a movie recommendation expert. When asked for movie suggestions, "
                                + "return a structured list of recommendations with title, year, genre, "
                                + "and a brief reason for each recommendation. Identify the overall theme.")
                .model(Settings.LLM_MODEL)
                .outputType("MovieList")
                .build();

        AgentResult result = runtime.run(
                agent,
                "Recommend 3 sci-fi movies that explore the concept of artificial intelligence.");
        result.printResult();

        runtime.shutdown();
    }
}
