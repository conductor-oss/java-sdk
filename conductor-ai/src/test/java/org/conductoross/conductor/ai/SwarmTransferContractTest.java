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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec R13 (T18): the swarm hand-off contract. Transfer tools echo the
 * hand-off {@code message}; {@code check_transfer} is first-wins with the
 * winning call's message on {@code transfer_message} and every non-winning
 * transfer surfaced in {@code dropped_transfers} (with a warning) — never
 * silently discarded.
 */
class SwarmTransferContractTest {

    private ListAppender<ILoggingEvent> logCapture;
    private Logger runtimeLogger;

    @BeforeEach
    void captureLogs() {
        runtimeLogger = (Logger) LoggerFactory.getLogger(AgentRuntime.class);
        logCapture = new ListAppender<>();
        logCapture.start();
        runtimeLogger.addAppender(logCapture);
    }

    @AfterEach
    void releaseLogs() {
        runtimeLogger.detachAppender(logCapture);
    }

    private static Map<String, Object> toolCall(String name, Map<String, Object> params) {
        Map<String, Object> call = new HashMap<>();
        call.put("name", name);
        call.put("inputParameters", params);
        return call;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> check(String agent, List<Object> toolCalls) {
        Function<Map<String, Object>, Object> handler = AgentRuntime.checkTransferHandler(agent);
        Map<String, Object> input = new HashMap<>();
        input.put("tool_calls", toolCalls);
        return (Map<String, Object>) handler.apply(input);
    }

    // ── transfer tool echo ───────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void transferToolEchoesTheHandOffMessage() {
        Map<String, Object> out =
                (Map<String, Object>) AgentRuntime.swarmTransferHandler(Map.of("message", "take over billing"));

        assertEquals(
                Map.of("message", "take over billing"),
                out,
                "COUNTERFACTUAL: the pre-fix worker returned emptyMap and the hand-off note vanished");
    }

    @Test
    @SuppressWarnings("unchecked")
    void transferToolWithoutMessageReturnsEmpty() {
        assertTrue(((Map<String, Object>) AgentRuntime.swarmTransferHandler(Map.of())).isEmpty());
        assertTrue(((Map<String, Object>) AgentRuntime.swarmTransferHandler(Map.of("message", ""))).isEmpty());
    }

    // ── check_transfer ───────────────────────────────────────────────────────

    @Test
    void noTransferProducesTheFalseShape() {
        Map<String, Object> out = check("researcher", null);

        assertEquals(false, out.get("is_transfer"));
        assertEquals("", out.get("transfer_to"));
        assertEquals("", out.get("transfer_message"));
        assertFalse(out.containsKey("dropped_transfers"));
    }

    @Test
    void singleTransferCarriesTheMessageAndNoDroppedKey() {
        Map<String, Object> out = check(
                "researcher",
                List.of(toolCall("researcher_transfer_to_writer", Map.of("message", "context for you"))));

        assertEquals(true, out.get("is_transfer"));
        assertEquals("writer", out.get("transfer_to"));
        assertEquals("context for you", out.get("transfer_message"));
        assertFalse(
                out.containsKey("dropped_transfers"),
                "dropped_transfers appears ONLY when more than one transfer was emitted");
    }

    @Test
    @SuppressWarnings("unchecked")
    void multipleTransfersFirstWinsRestDroppedWithWarning() {
        Map<String, Object> out = check(
                "researcher",
                List.of(
                        toolCall("researcher_transfer_to_writer", Map.of("message", "first note")),
                        toolCall("researcher_transfer_to_editor", Map.of("message", "second note"))));

        assertEquals(true, out.get("is_transfer"));
        assertEquals("writer", out.get("transfer_to"), "first transfer wins");
        assertEquals("first note", out.get("transfer_message"));

        List<Map<String, Object>> dropped = (List<Map<String, Object>>) out.get("dropped_transfers");
        assertEquals(1, dropped.size());
        assertEquals("editor", dropped.get(0).get("transfer_to"));
        assertEquals("second note", dropped.get(0).get("message"));

        boolean warned = logCapture.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("writer")
                        && event.getFormattedMessage().contains("editor"));
        assertTrue(warned, "the drop must be warned, naming honored and dropped targets");
    }

    @Test
    void missingMessageBecomesEmptyString() {
        Map<String, Object> out =
                check("researcher", List.of(toolCall("researcher_transfer_to_writer", Map.of())));

        assertEquals("", out.get("transfer_message"));
    }

    @Test
    void argumentsKeyVariantIsTolerated() {
        Map<String, Object> call = new HashMap<>();
        call.put("name", "researcher_transfer_to_writer");
        call.put("arguments", Map.of("message", "via arguments"));

        Map<String, Object> out = check("researcher", List.of(call));

        assertEquals("via arguments", out.get("transfer_message"));
    }

    @Test
    void nestedFunctionNameVariantIsTolerated() {
        Map<String, Object> call = new HashMap<>();
        call.put("function", Map.of("name", "researcher_transfer_to_writer"));
        call.put("inputParameters", Map.of("message", "nested shape"));

        Map<String, Object> out = check("researcher", List.of(call));

        assertEquals(true, out.get("is_transfer"));
        assertEquals("writer", out.get("transfer_to"));
        assertEquals("nested shape", out.get("transfer_message"));
    }

    @Test
    void otherAgentsTransfersAreIgnored() {
        Map<String, Object> out = check(
                "researcher", List.of(toolCall("writer_transfer_to_editor", Map.of("message", "not mine"))));

        assertEquals(false, out.get("is_transfer"), "per-agent worker only matches its own prefix");
    }
}
