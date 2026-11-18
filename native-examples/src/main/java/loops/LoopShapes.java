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
package loops;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.conductoross.conductor.sdk.Conductor.forEach;
import static org.conductoross.conductor.sdk.Conductor.repeatUntil;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.workflowId;

/**
 * Loops, each shape written twice: in plain Java, then with the SDK's loop construct, as {@code
 * primitives.SwitchCase} does for {@code if}/{@code switch} and {@code switchOn}. Both forms make
 * the same calls; they differ in who runs the loop and in what the execution shows.
 *
 * <pre>
 *   plain Java                          the code runs the loop; each call is a task of its own. The
 *   while, do-while, for, for-each,     diagram draws a loop around the calls inside it, named after
 *   break, continue                     the class and line (LoopShapes_loop_104), one pass per round.
 *
 *   doWhile(name, body, condition)      the code runs the loop too, as a Java do-while: the body,
 *                                       then again while the condition holds. The diagram draws it
 *                                       under the name you give. Body and condition may call tasks.
 *                                       The body always runs once: a loop that may run zero times
 *                                       stays a plain while.
 *
 *   forEach(name, items, tasks::m)      the server runs the loop: a DO_WHILE task calling m once per
 *                                       item, in order. The code wakes once, with all the results.
 *
 *   repeatUntil(call, until, backoff)   the server runs the loop: the call, a wait, the call again,
 *                                       until {@code until} holds or the attempts run out. The code
 *                                       wakes once, with the last result.
 * </pre>
 *
 * Use plain Java by default. Reach for doWhile to name the loop, and for forEach or repeatUntil
 * when one task repeats many times and waking the code for each call is not worth it.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" loops.LoopShapes [worker]
 * </pre>
 */
public class LoopShapes {

    public record Status(int poll, boolean done) {}

    public static class Tasks {
        @WorkerTask("loop_step")
        public String step(@InputParam("label") String label) {
            return "did " + label;
        }

        @WorkerTask("loop_ready")
        public boolean ready(@InputParam("round") int round) {
            return round >= 2; // ready on the second round
        }

        private final java.util.Map<String, Integer> polls = new java.util.concurrent.ConcurrentHashMap<>();

        /** Done on the third poll of a key. */
        @WorkerTask("shape_poll")
        public Status poll(@InputParam("key") String key) {
            int n = polls.merge(key, 1, Integer::sum);
            return new Status(n, n >= 3);
        }
    }

    private final Tasks tasks;

    public LoopShapes(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "loop_shapes")
    public List<String> run(@InputParam("n") int n) {
        List<String> out = new ArrayList<>();

        // while: checked before each round, so it may run zero times. No construct form: doWhile
        // always runs its body once.
        int i = 0;
        while (i < n) {
            out.add(tasks.step("while " + i));
            i++;
        }

        // do-while whose condition calls a task ...
        int round = 0;
        do {
            round++;
            out.add(tasks.step("do " + round));
        } while (!tasks.ready(round));

        // ... and the same with doWhile: the loop is drawn as "until_ready"
        int[] pass = {0};
        doWhile("until_ready", () -> {
            pass[0]++;
            out.add(tasks.step("until_ready " + pass[0]));
        }, () -> !tasks.ready(pass[0]));

        // for (;;) with a break ...
        int k = 0;
        for (;;) {
            out.add(tasks.step("for " + k));
            k++;
            if (k == 3) {
                break;
            }
        }

        // ... and the same with doWhile: the break becomes the condition
        int[] count = {0};
        doWhile("three_rounds", () -> {
            out.add(tasks.step("three_rounds " + count[0]));
            count[0]++;
        }, () -> count[0] < 3);

        // for-each over a list, one task per item ...
        for (String item : List.of("a", "b")) {
            out.add(tasks.step(item));
        }

        // ... and the same with forEach: one DO_WHILE task on the server, the code wakes once
        out.addAll(forEach("labels", List.of("x", "y"), tasks::step));

        // polling: call, wait a second, call again until done ...
        String key = workflowId();
        Status status = tasks.poll(key + "-java");
        while (!status.done()) {
            sleep(Duration.ofSeconds(1));
            status = tasks.poll(key + "-java");
        }
        out.add("polled " + status.poll());

        // ... and the same with repeatUntil: the server polls, the code wakes once
        Status polled = repeatUntil(() -> tasks.poll(key + "-server"), Status::done,
                Backoff.every(Duration.ofSeconds(1)).attempts(10));
        out.add("polled " + polled.poll());

        return out;
    }

    static final List<String> EXPECTED = List.of(
            "did while 0", "did while 1",
            "did do 1", "did do 2", "did until_ready 1", "did until_ready 2",
            "did for 0", "did for 1", "did for 2", "did three_rounds 0", "did three_rounds 1", "did three_rounds 2",
            "did a", "did b", "did x", "did y",
            "polled 3", "polled 3");

    /** {@code worker}: host the code and its tasks only, for runs started on the server. */
    public static void main(String[] args) throws InterruptedException {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.workflows(LoopShapes.class).tasks(new Tasks()).start();
        if (args.length > 0 && args[0].equals("worker")) {
            System.out.println("loop_shapes worker started");
            Thread.currentThread().join();
        }
        WorkflowHandle<List<String>> run = conductor.start(LoopShapes::run, 2);
        System.out.println("execution: " + run.workflowId());
        List<String> result = run.result(Duration.ofSeconds(60));
        System.out.println("result:    " + result);
        boolean ok = EXPECTED.equals(result);
        System.out.println(ok ? "LOOPSHAPES PASSED" : "LOOPSHAPES FAILED: expected " + EXPECTED);
        conductor.close();
        client.shutdown();
        System.exit(ok ? 0 : 1);
    }
}
