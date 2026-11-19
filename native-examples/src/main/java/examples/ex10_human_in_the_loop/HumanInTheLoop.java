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
package examples.ex10_human_in_the_loop;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.external;
import static org.conductoross.conductor.sdk.Conductor.human;

/**
 * 10 Human in the loop: waiting for a person, or for another system, to answer.
 *
 * <pre>
 *   human(ref).assignee(who)            a HUMAN task: a person answers in the Conductor UI or
 *     .input(map)                       through the API. The answer comes back as the type given;
 *     .await(Type.class, timeout)       empty when nobody answers in time.
 *   external(ref)                       a WAIT task that any system completes, for example
 *     .await(Type.class, timeout)       POST /api/tasks/{workflowId}/{ref}/COMPLETED with the
 *                                       output as the JSON body
 *   run.completeTask(ref, output, t)    the same from Java: completes the task once the
 *                                       workflow reaches it
 * </pre>
 *
 * The workflow may wait days for an answer; nothing is held meanwhile.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex10_human_in_the_loop
 * </pre>
 *
 * This main answers both waits itself, as a manager and a bank would.
 */
public class HumanInTheLoop {

    public record Expense(String id, String employee, String manager, int amount, String reason) {}

    /** What the manager answers. */
    public record Decision(boolean approved, String comment) {}

    /** What the bank sends when the money has moved. */
    public record Transfer(String reference) {}

    public static class Tasks {

        @WorkerTask("request_transfer")
        public String requestTransfer(String employee, int amount) {
            return "transfer of " + amount + " to " + employee + " requested";
        }
    }

    private final Tasks tasks;

    public HumanInTheLoop(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "expense_approval")
    public String approve(Expense expense) {
        if (expense.amount() <= 100) {
            tasks.requestTransfer(expense.employee(), expense.amount());
            return expense.id() + ": approved without review";
        }

        // a person decides; after three days without an answer, escalate
        Optional<Decision> decision = human("manager_review").assignee(expense.manager())
                .input(Map.of("employee", expense.employee(), "amount", expense.amount(), "reason", expense.reason()))
                .await(Decision.class, Duration.ofDays(3));
        if (decision.isEmpty()) {
            return expense.id() + ": no answer in three days, escalated";
        }
        if (!decision.get().approved()) {
            return expense.id() + ": rejected, " + decision.get().comment();
        }

        // ask the bank, then wait for the bank to say the money moved
        tasks.requestTransfer(expense.employee(), expense.amount());
        Optional<Transfer> transfer = external("bank_confirmation").await(Transfer.class, Duration.ofDays(5));
        return expense.id() + ": approved (" + decision.get().comment() + "), paid as "
                + transfer.map(Transfer::reference).orElse("unknown: the bank did not confirm");
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(HumanInTheLoop.class).tasks(new Tasks()).start();

            Expense expense = new Expense("EXP-7", "ana", "mia", 840, "conference travel");
            WorkflowHandle<String> run = conductor.start(HumanInTheLoop::approve, expense);
            System.out.println("execution: " + run.workflowId());

            run.completeTask("manager_review", new Decision(true, "have a good trip"), Duration.ofSeconds(30));
            run.completeTask("bank_confirmation", new Transfer("TX-2291"), Duration.ofSeconds(30));
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
