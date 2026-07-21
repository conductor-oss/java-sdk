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

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.WorkflowSchedule;

/**
 * Example 99 — Scheduled Agent
 *
 * <p>Deploys an agent, creates two native Conductor schedules, and exercises
 * the scheduler lifecycle: list, pause, resume, preview, and cleanup.
 *
 * <p>Usage:
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:6767/api \
 *   CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini \
 *   ./gradlew :examples:run -PmainClass=org.conductoross.conductor.ai.examples.Example99ScheduledAgent
 * </pre>
 */
public class Example99ScheduledAgent {

    public static void main(String[] args) throws Exception {
        String model = System.getenv().getOrDefault("CONDUCTOR_AGENT_LLM_MODEL", "anthropic/claude-sonnet-4-6");

        Agent agent = Agent.builder()
                .name("eng_digest_99")
                .model(model)
                .instructions(
                        "You are a concise engineering digest writer. " +
                        "Summarise recent activity for the channel in your input " +
                        "and return a short markdown bullet list (max 5 items).")
                .build();

        try (AgentRuntime runtime = new AgentRuntime()) {

            // 1. Deploy the agent, then use the typed SchedulerClient directly.
            runtime.deploy(agent);
            SchedulerClient schedules = runtime.getSchedulerClient();
            String weekdayName = agent.getName() + "_weekday";
            String fridayName = agent.getName() + "_friday";
            saveSchedule(
                    schedules,
                    weekdayName,
                    agent.getName(),
                    "0 0 9 * * MON-FRI",
                    Map.of("channel", "#eng"),
                    "Weekday morning digest");
            saveSchedule(
                    schedules,
                    fridayName,
                    agent.getName(),
                    "0 0 17 * * FRI",
                    Map.of("channel", "#all-hands", "mode", "weekly"),
                    "Weekly all-hands digest");
            System.out.printf("✓ Deployed '%s' with 2 schedules%n", agent.getName());

            // 2. List schedules for this agent.
            List<WorkflowSchedule> infos = schedules.getAllSchedules(agent.getName());
            System.out.printf("%nSchedules (%d):%n", infos.size());
            for (WorkflowSchedule s : infos) {
                System.out.printf("  %s  %s  [%s]%n",
                        s.getName(), s.getCronExpression(), s.isPaused() ? "PAUSED" : "active");
            }

            if (infos.size() < 2) {
                System.err.println("Expected 2 schedules; aborting.");
                return;
            }

            // 3. Pause the weekday schedule.
            schedules.pauseSchedule(weekdayName, "rate-limit cooldown demo");
            WorkflowSchedule afterPause = schedules.getSchedule(weekdayName);
            System.out.printf("%n✓ Paused '%s': paused=%b, reason=%s%n",
                    weekdayName, afterPause.isPaused(), afterPause.getPausedReason());

            // 4. Resume it.
            schedules.resumeSchedule(weekdayName);
            WorkflowSchedule afterResume = schedules.getSchedule(weekdayName);
            System.out.printf("✓ Resumed '%s': paused=%b%n", weekdayName, afterResume.isPaused());

            // 5. Preview next 5 fire times for the weekday cron.
            List<Long> nextFires = schedules.getNextFewSchedules("0 0 9 * * MON-FRI", null, null, 5);
            System.out.println("\nNext 5 fires for weekday:");
            for (int i = 0; i < nextFires.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, new java.util.Date(nextFires.get(i)));
            }

            // 6. Cleanup: remove each schedule explicitly.
            schedules.deleteSchedule(weekdayName);
            schedules.deleteSchedule(fridayName);
            System.out.printf("%n✓ Purged all schedules for '%s'%n", agent.getName());
        }
    }

    private static void saveSchedule(
            SchedulerClient schedules,
            String scheduleName,
            String workflowName,
            String cron,
            Map<String, Object> input,
            String description) {
        StartWorkflowRequest workflow = new StartWorkflowRequest();
        workflow.setName(workflowName);
        workflow.setInput(input);
        schedules.saveSchedule(new SaveScheduleRequest()
                .name(scheduleName)
                .cronExpression(cron)
                .zoneId("America/Los_Angeles")
                .startWorkflowRequest(workflow)
                .description(description));
    }
}
