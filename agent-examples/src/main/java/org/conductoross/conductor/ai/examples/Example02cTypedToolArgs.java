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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 02c — Typed Tool Arguments
 *
 * <p>Demonstrates {@code @Tool} parameters declared as {@code java.time}
 * types rather than {@code String}. The SDK emits a JSON Schema with the
 * appropriate {@code format} ({@code "date-time"}, {@code "duration"}) so
 * the LLM knows to send ISO-8601 strings, and the internal {@code JsonMapper}
 * parses them back into typed values before the method runs. The tool bodies
 * therefore work with real {@link LocalDateTime}, {@link Instant}, and
 * {@link Duration} instances.
 */
public class Example02cTypedToolArgs {

    static class CalendarTools {

        @Tool(name = "schedule_meeting",
              description = "Schedule a meeting. Pass an ISO-8601 local date-time "
                  + "(e.g. 2026-05-12T14:00:00) and a duration like PT1H.")
        public Map<String, Object> scheduleMeeting(LocalDateTime start, Duration duration) {
            // start and duration arrive as real types — not strings.
            return Map.of(
                "starts_at", start.toString(),
                "ends_at", start.plus(duration).toString(),
                "duration_minutes", duration.toMinutes()
            );
        }

        @Tool(name = "record_event",
              description = "Record an event timestamp.")
        public Map<String, Object> recordEvent(Instant when) {
            return Map.of("recorded_at", when.toString(), "epoch_second", when.getEpochSecond());
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> tools = ToolRegistry.fromInstance(new CalendarTools());

        Agent agent = Agent.builder()
            .name("calendar_assistant")
            .model(Settings.LLM_MODEL)
            .tools(tools)
            .instructions(
                "You are a calendar assistant. Use the tools to schedule meetings "
                + "and record events. Pass dates and times exactly as the user gives them.")
            .build();

        AgentResult result = runtime.run(agent,
            "Schedule a one-hour meeting starting May 12th, 2026 at 2 PM, "
            + "then record an event at 2026-05-12T13:45:00Z.");
        result.printResult();

        runtime.shutdown();
    }
}
