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
package org.conductoross.conductor.ai.schedule;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.WorkflowSchedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for Schedule + Schedules helpers (no network). */
class ScheduleTest {

    // ── Schedule construction ─────────────────────────────────────────

    @Test
    void minimal() {
        Schedule s = Schedule.builder().name("daily").cron("0 0 9 * * ?").build();
        assertEquals("daily", s.getName());
        assertEquals("UTC", s.getTimezone());
        assertFalse(s.isCatchup());
        assertFalse(s.isPaused());
        assertTrue(s.getInput().isEmpty());
    }

    @Test
    void full() {
        Map<String, Object> input = new HashMap<>();
        input.put("c", "#eng");
        Schedule s = Schedule.builder()
                .name("w")
                .cron("0 0 9 * * MON")
                .timezone("America/Los_Angeles")
                .input(input)
                .catchup(true)
                .paused(true)
                .startAt(1000L)
                .endAt(2000L)
                .description("desc")
                .build();
        assertEquals("America/Los_Angeles", s.getTimezone());
        assertEquals(input, s.getInput());
        assertTrue(s.isCatchup());
        assertTrue(s.isPaused());
        assertEquals(1000L, s.getStartAt());
        assertEquals(2000L, s.getEndAt());
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(
                ScheduleException.class,
                () -> Schedule.builder().name("").cron("* * * * * ?").build());
        assertThrows(
                ScheduleException.class,
                () -> Schedule.builder().name("  ").cron("* * * * * ?").build());
    }

    @Test
    void rejectsEmptyCron() {
        assertThrows(
                ScheduleException.class,
                () -> Schedule.builder().name("x").cron("").build());
    }

    @Test
    void rejectsInvertedWindow() {
        assertThrows(ScheduleException.class, () -> Schedule.builder()
                .name("x")
                .cron("* * * * * ?")
                .startAt(2000L)
                .endAt(1000L)
                .build());
        assertThrows(ScheduleException.class, () -> Schedule.builder()
                .name("x")
                .cron("* * * * * ?")
                .startAt(1000L)
                .endAt(1000L)
                .build());
    }

    // ── Wire-name prefix/unprefix ─────────────────────────────────────

    @Test
    void prefixRoundtrips() {
        assertEquals("digest-daily", Schedules.prefix("digest", "daily"));
        assertEquals("daily", Schedules.unprefix("digest", "digest-daily"));
    }

    @Test
    void unprefixNoMatchReturnsInput() {
        assertEquals("unrelated", Schedules.unprefix("agent", "unrelated"));
    }

    @Test
    void agentNameWithHyphen() {
        String wire = Schedules.prefix("my-agent", "daily");
        assertEquals("my-agent-daily", wire);
        assertEquals("daily", Schedules.unprefix("my-agent", wire));
    }

    // ── Payload mapping ───────────────────────────────────────────────

    @Test
    void toSaveRequestMinimal() {
        Schedule s = Schedule.builder().name("daily").cron("0 0 9 * * ?").build();
        SaveScheduleRequest req = Schedules.toSaveRequest(s, "digest");
        assertEquals("digest-daily", req.getName());
        assertEquals("0 0 9 * * ?", req.getCronExpression());
        assertEquals("UTC", req.getZoneId());
        assertEquals(false, req.isPaused());
        assertEquals(false, req.isRunCatchupScheduleInstances());
        StartWorkflowRequest swr = req.getStartWorkflowRequest();
        assertEquals("digest", swr.getName());
        assertTrue(swr.getInput().isEmpty());
    }

    @Test
    void toSaveRequestFull() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("c", "#eng");
        input.put("n", 42);
        Schedule s = Schedule.builder()
                .name("w")
                .cron("0 0 9 * * MON")
                .timezone("America/Los_Angeles")
                .input(input)
                .catchup(true)
                .paused(true)
                .startAt(1000L)
                .endAt(2000L)
                .description("desc")
                .build();
        SaveScheduleRequest req = Schedules.toSaveRequest(s, "digest");
        assertEquals("America/Los_Angeles", req.getZoneId());
        assertEquals(true, req.isPaused());
        assertEquals(true, req.isRunCatchupScheduleInstances());
        assertEquals(1000L, req.getScheduleStartTime());
        assertEquals(2000L, req.getScheduleEndTime());
        assertEquals("desc", req.getDescription());
    }

    @Test
    void inputCopiedNotShared() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("a", 1);
        Schedule s =
                Schedule.builder().name("x").cron("* * * * * ?").input(original).build();
        SaveScheduleRequest req = Schedules.toSaveRequest(s, "agent");
        Map<String, Object> swrInput = req.getStartWorkflowRequest().getInput();
        swrInput.put("mutated", true);
        assertNull(original.get("mutated"));
    }

    @Test
    void fromWorkflowScheduleBasic() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("c", "#eng");
        StartWorkflowRequest swr = new StartWorkflowRequest();
        swr.setName("digest");
        swr.setInput(input);

        WorkflowSchedule ws = WorkflowSchedule.builder()
                .name("digest-daily")
                .cronExpression("0 0 9 * * ?")
                .zoneId("UTC")
                .paused(false)
                .startWorkflowRequest(swr)
                .createTime(111L)
                .createdBy("alice")
                .build();

        ScheduleInfo info = Schedules.fromWorkflowSchedule(ws, "digest");
        assertEquals("digest-daily", info.getName());
        assertEquals("daily", info.getShortName());
        assertEquals("digest", info.getAgent());
        assertEquals("0 0 9 * * ?", info.getCron());
        assertFalse(info.isPaused());
        assertEquals(input, info.getInput());
        assertEquals(111L, info.getCreateTime());
        assertEquals("alice", info.getCreatedBy());
    }

    @Test
    void fromWorkflowScheduleDerivesAgentWhenOmitted() {
        StartWorkflowRequest swr = new StartWorkflowRequest();
        swr.setName("digest");

        WorkflowSchedule ws = WorkflowSchedule.builder()
                .name("digest-daily")
                .startWorkflowRequest(swr)
                .build();

        ScheduleInfo info = Schedules.fromWorkflowSchedule(ws, null);
        assertEquals("digest", info.getAgent());
        assertEquals("daily", info.getShortName());
    }

    // ── Unique-name validation ────────────────────────────────────────

    @Test
    void distinctNamesOk() {
        Schedules.checkUniqueNames(List.of(
                Schedule.builder().name("a").cron("* * * * * ?").build(),
                Schedule.builder().name("b").cron("* * * * * ?").build()));
    }

    @Test
    void duplicateNameRaises() {
        assertThrows(
                ScheduleException.NameConflict.class,
                () -> Schedules.checkUniqueNames(List.of(
                        Schedule.builder().name("a").cron("* * * * * ?").build(),
                        Schedule.builder().name("a").cron("0 0 9 * * ?").build())));
    }
}
