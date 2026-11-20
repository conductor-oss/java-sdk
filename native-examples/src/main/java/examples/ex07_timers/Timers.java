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
package examples.ex07_timers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Schedule;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.sleep;

/**
 * 07 Timers: waiting, for seconds or for months.
 *
 * <p>The server keeps the clock. A timer is a WAIT task, and no thread or worker is held while it
 * runs, so a sleep of thirty days costs no more than a sleep of one second, and it survives restarts
 * of the server and of your workers.
 *
 * <pre>
 *   sleep(duration)              wait this long
 *   sleepUntil(instant)          wait until then; the server resolves it to the minute
 *   now()                        the time, the same each time the code runs again: use it, never
 *                                Instant.now(), in workflow code
 *   @Schedule(cron = ...)        start the workflow on a schedule; registered with the server's
 *                                scheduler when the workflow is hosted
 * </pre>
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex07_timers
 * </pre>
 */
public class Timers {

    /** Who to remind, how often and how many times; a real trial would wait days, not seconds. */
    public record Trial(String user, int reminders, int everySeconds) {}

    public static class Tasks {

        @WorkerTask("send_reminder")
        public String sendReminder(String user, int reminder) {
            return "reminder " + reminder + " sent to " + user;
        }
    }

    private final Tasks tasks;

    public Timers(Tasks tasks) {
        this.tasks = tasks;
    }

    // registered paused, so that it shows in the UI's schedules without starting runs on its own
    @Schedule(cron = "0 0 9 * * MON-FRI", zone = "UTC", name = "trial_reminders_weekdays",
            input = "{\"user\": \"everyone\", \"reminders\": 1, \"everySeconds\": 1}", paused = true)
    @Workflow(name = "trial_reminders")
    public List<String> remind(Trial trial) {
        Instant started = now();
        List<String> sent = new ArrayList<>();
        for (int n = 1; n <= trial.reminders(); n++) {
            sleep(Duration.ofSeconds(trial.everySeconds()));
            sent.add(tasks.sendReminder(trial.user(), n));
        }
        sent.add("waited " + Duration.between(started, now()).toSeconds() + "s in all");
        return sent;
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Timers.class).tasks(new Tasks()).start();

            WorkflowHandle<List<String>> run = conductor.start(Timers::remind, new Trial("ana", 3, 2));
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofMinutes(1)));
        }
        client.shutdown();
    }
}
