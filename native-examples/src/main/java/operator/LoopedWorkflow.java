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
package operator;

import java.time.Duration;
import java.util.List;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Workflow;

import static org.conductoross.conductor.sdk.Conductor.forEach;
import static org.conductoross.conductor.sdk.Conductor.repeatUntil;

/** The two loops the server runs for code: one task per item, and polling until done. For reruns. */
public class LoopedWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public LoopedWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    public record Poll(boolean done, int check) {}

    @Workflow(name = "native_looped", version = 1)
    public String run(String id) {
        List<String> labels = forEach("items", List.of(id + "-p", id + "-q", id + "-r"), tasks::loopItem);
        Poll polled = repeatUntil(
                () -> tasks.loopPoll(id),
                Poll::done,
                Backoff.every(Duration.ofSeconds(1)).attempts(10));
        return labels.size() + " labels/" + polled.done() + "/" + tasks.loopAfter(id);
    }
}
