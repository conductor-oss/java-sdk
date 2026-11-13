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
package failures;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.conductoross.conductor.sdk.Conductor.async;

/**
 * What an unchecked exception thrown by workflow code does.
 *
 * <ul>
 *   <li>Thrown by the workflow method, it fails the workflow: status FAILED, with the exception
 *       as the reason. Tasks that already ran stay as they are, a FAILED task named after the
 *       exception marks where the code threw, at its line, and what the code would have called
 *       next shows after it as a preview ({@code failures_ship}). A retry runs the code again, so a build
 *       with the bug fixed continues the execution. ({@code failures_validate})
 *   <li>Thrown in an {@code async} lane, it fails the workflow only when the code waits for that
 *       lane with {@code join} or {@code get}. A lane nothing waits for does not: the workflow
 *       completes, and the exception is written to the log of the task the lane ran last.
 *       ({@code failures_background})
 * </ul>
 *
 * Run against a server ({@code CONDUCTOR_SERVER_URL}); it prints each execution's status, its
 * reason, and the task log.
 */
public class Failures {

    public record Order(String id, int quantity) {}

    public static class Tasks {
        @WorkerTask("failures_lookup")
        public Order lookup(String orderId) {
            return new Order(orderId, orderId.endsWith("-0") ? 0 : 3);
        }

        @WorkerTask("failures_ship")
        public String ship(Order order) {
            return "shipped " + order.quantity() + " of " + order.id();
        }

        @WorkerTask("failures_audit")
        public String audit(String orderId) {
            return "audited " + orderId;
        }
    }

    private final Tasks tasks;

    public Failures(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "failures_validate")
    public String validate(String orderId) {
        Order order = tasks.lookup(orderId);
        if (order.quantity() <= 0) {
            // unchecked: the workflow fails, with this as its reason
            throw new IllegalArgumentException("order " + orderId + " has no items");
        }
        return tasks.ship(order);
    }

    @Workflow(name = "failures_background")
    public String background(String orderId) {
        // nothing joins this lane: its exception does not fail the workflow, it goes to the task log
        async(() -> {
            tasks.audit(orderId);
            throw new IllegalStateException("audit of " + orderId + " found a mismatch");
        });
        return tasks.ship(tasks.lookup(orderId));
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Failures.class).tasks(new Tasks()).start();

        report(conductor, conductor.start(Failures::validate, "ord-0"));
        report(conductor, conductor.start(Failures::validate, "ord-1"));
        report(conductor, conductor.start(Failures::background, "ord-1"));

        conductor.close();
        client.shutdown();
    }

    @SuppressWarnings("unchecked")
    private static void report(Conductor conductor, WorkflowHandle<String> run) {
        String outcome;
        try {
            outcome = "result: " + run.result(Duration.ofSeconds(30));
        } catch (RuntimeException e) {
            outcome = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        Map<String, Object> wf = get(conductor, "/workflow/" + run.workflowId(), true, new TypeReference<>() {});
        System.out.println(wf.get("workflowName") + " " + run.workflowId());
        System.out.println("  status " + wf.get("status") + ", " + outcome);
        for (Map<String, Object> task : (List<Map<String, Object>>) wf.get("tasks")) {
            System.out.println("  task " + task.get("referenceTaskName") + " " + task.get("status"));
            List<Map<String, Object>> logs = get(conductor, "/tasks/" + task.get("taskId") + "/log", false, new TypeReference<>() {});
            for (Map<String, Object> log : logs == null ? List.<Map<String, Object>>of() : logs) {
                System.out.println("    log: " + log.get("log"));
            }
        }
    }

    private static <T> T get(Conductor conductor, String path, boolean includeTasks, TypeReference<T> type) {
        ConductorClientRequest.Builder request =
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path(path);
        if (includeTasks) {
            request.addQueryParam("includeTasks", true);
        }
        return conductor.client().execute(request.build(), type).getData();
    }
}
