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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.conductoross.conductor.sdk.Conductor.forEach;
import static org.conductoross.conductor.sdk.Conductor.repeatUntil;

/**
 * Loops. Three kinds, by who runs the loop:
 *
 * <pre>
 *   plain Java: for, for-each, while,   the code runs the loop; each call is a task of its own and the
 *   do-while, break, continue           diagram draws a loop around the calls inside, named after the
 *                                       line it is on. Loops nest. Use these by default.
 *
 *   doWhile(name, body, condition)      the same, with a name you choose for the loop the diagram
 *                                       draws. The body runs at least once; it may call anything.
 *
 *   forEach(name, items, tasks::m)      the server runs the loop: one task per item, in order, and
 *   repeatUntil(body, until, backoff)   the code wakes once, when it is done. For long loops of one
 *                                       task, polling with backoff included.
 * </pre>
 */
public class Loops {

    public record Status(int check, boolean done) {}

    public static class Tasks {
        @WorkerTask("p_step")
        public String step(String label) {
            return label;
        }

        /** Done on the third check of a key. */
        @WorkerTask("p_check")
        public Status check(String key, int check) {
            return new Status(check, check >= 3);
        }

        private final Map<String, Integer> polls = new ConcurrentHashMap<>();

        /** Done on the third poll of a key; counts per worker, so a polling loop needs no input change. */
        @WorkerTask("p_poll")
        public Status poll(String key) {
            int n = polls.merge(key, 1, Integer::sum);
            return new Status(n, n >= 3);
        }
    }

    private final Tasks tasks;

    public Loops(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "p_loops")
    public List<String> run(String key) {
        List<String> out = new ArrayList<>();

        // for: a counted loop
        for (int i = 1; i <= 2; i++) {
            out.add(tasks.step("for-" + i));
        }

        // for-each over a list, with continue
        for (String item : List.of("a", "skip", "b")) {
            if (item.equals("skip")) {
                continue;
            }
            out.add(tasks.step("each-" + item));
        }

        // while, its condition calling a task
        int check = 1;
        while (!tasks.check(key, check).done()) {
            check++;
        }
        out.add("checked " + check);

        // do-while with break
        int round = 0;
        do {
            round++;
            out.add(tasks.step("do-" + round));
            if (round == 2) {
                break;
            }
        } while (true);

        // nested loops: the inner one is drawn inside the outer one
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 2; col++) {
                out.add(tasks.step(row + "x" + col));
            }
        }

        // doWhile: a loop with a name of your choosing ("retry_until_ok")
        int[] attempt = {0};
        doWhile("retry_until_ok", () -> {
            attempt[0]++;
            out.add(tasks.step("attempt-" + attempt[0]));
        }, () -> attempt[0] < 3);

        // forEach: the server runs one p_step per item, the code wakes once
        out.addAll(forEach("labels", List.of("x", "y", "z"), tasks::step));

        // repeatUntil: the server polls p_poll every second until done, up to 10 times
        Status polled = repeatUntil(() -> tasks.poll(key), Status::done,
                Backoff.every(Duration.ofSeconds(1)).attempts(10));
        out.add("polled " + polled.check());

        return out;
    }
}
