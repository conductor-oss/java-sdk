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
package primitives;

import java.time.Duration;
import java.time.Instant;

import org.conductoross.conductor.sdk.Schedule;
import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.awaitCondition;
import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.sleepUntil;

/**
 * Time. The server keeps the clock: a timer is a WAIT task, and nothing holds a thread meanwhile,
 * so a sleep of a month costs the same as one of a second.
 *
 * <pre>
 *   sleep(duration)                      wait this long
 *   sleepUntil(instant)                  wait until then; the server resolves it to the minute,
 *                                        so it ends at the start of the minute after, at the latest
 *   awaitCondition(() -> ..., timeout)   wait until a handler makes the condition true, or the
 *                                        timeout passes: false then (see Messages)
 *   now()                                the time, the same on every re-run of the code; never
 *                                        Instant.now() in a workflow
 *   @Schedule(cron = ...)                start the workflow on a schedule, registered with the
 *                                        server's scheduler when the host starts
 * </pre>
 */
public class Timers {

    public static class Tasks {
        @WorkerTask("p_tick")
        public String tick(int n) {
            return "tick " + n;
        }
    }

    private final Tasks tasks;

    public Timers(Tasks tasks) {
        this.tasks = tasks;
    }

    // paused, so it is registered for you to look at and does not start runs on its own
    @Schedule(cron = "0 0 9 * * MON-FRI", zone = "UTC", name = "p_timers_weekdays", input = "{\"ticks\": 2}", paused = true)
    @Workflow(name = "p_timers")
    public String run(Ticks input) {
        Instant start = now();

        // a timer in a loop: one WAIT per pass
        for (int n = 1; n <= input.ticks(); n++) {
            sleep(Duration.ofSeconds(1));
            tasks.tick(n);
        }

        // until an instant: here the start of the minute after start + 4s
        sleepUntil(start.plusSeconds(4));

        // nothing will make this true: it gives up after two seconds
        boolean never = awaitCondition(() -> false, Duration.ofSeconds(2));

        long waited = Duration.between(start, now()).toSeconds();
        return (waited >= 4 ? "waited" : "early " + waited) + (never ? " and saw it" : " and timed out");
    }

    public record Ticks(int ticks) {}
}
