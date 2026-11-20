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
package examples.ex03_loops;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.conductoross.conductor.sdk.Conductor.forEach;
import static org.conductoross.conductor.sdk.Conductor.repeatUntil;
import static org.conductoross.conductor.sdk.Conductor.sleep;

/**
 * 03 Loops: each loop written twice, in plain Java and with Conductor's loop functions.
 *
 * <pre>
 *   plain Java: for, for-each, while,     your code runs the loop. Each call in it is a task of its
 *   do-while, break, continue             own, and the diagram draws a loop around the calls. Use
 *                                         these by default.
 *
 *   doWhile(name, body, condition)        the same as do-while, with a name of your choosing for the
 *                                         loop in the diagram. The body runs at least once.
 *
 *   forEach(name, items, tasks::method)   the server runs the loop: one task per item, one after
 *                                         another, and your code wakes once, when all are done.
 *   repeatUntil(call, until, backoff)     the server repeats one call until its result passes the
 *                                         test, waiting between calls. Polling, mostly.
 * </pre>
 *
 * Reach for {@code forEach} and {@code repeatUntil} when a loop runs one task many times (a long
 * list, a poll that may take hundreds of checks): the server does the passes without waking the
 * code for each one.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex03_loops
 * </pre>
 */
public class Loops {

    public record Batch(String id, List<String> items) {}

    public record ExportStatus(String job, int check, boolean done) {}

    public record Report(List<String> packed, List<String> packedByServer, int checks, int checksByServer,
                         List<String> waves, List<String> wavesByDoWhile) {}

    public static class Tasks {

        private final Map<String, Integer> checks = new ConcurrentHashMap<>();

        @WorkerTask("pack_item")
        public String pack(String item) {
            return "packed " + item;
        }

        @WorkerTask("start_export")
        public String startExport(String batchId) {
            return batchId + "-export";
        }

        /** An export that is done on its third check. */
        @WorkerTask("export_status")
        public ExportStatus exportStatus(String job) {
            int check = checks.merge(job, 1, Integer::sum);
            return new ExportStatus(job, check, check >= 3);
        }

        @WorkerTask("pick_wave")
        public String pickWave(int wave) {
            return "wave " + wave;
        }
    }

    private final Tasks tasks;

    public Loops(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "loops")
    public Report run(Batch batch) {
        // 1. one task per item. In plain Java:
        List<String> packed = new ArrayList<>();
        for (String item : batch.items()) {
            packed.add(tasks.pack(item));
        }
        // ... and with forEach, the server running the loop:
        List<String> packedByServer = forEach("pack_items", batch.items(), tasks::pack);

        // 2. polling until a job is done. In plain Java, waiting a second between checks:
        String job = tasks.startExport(batch.id() + "-a");
        int checks = 1;
        while (!tasks.exportStatus(job).done()) {
            sleep(Duration.ofSeconds(1));
            checks++;
        }
        // ... and with repeatUntil: up to 10 checks, a second apart
        String job2 = tasks.startExport(batch.id() + "-b");
        ExportStatus status = repeatUntil(() -> tasks.exportStatus(job2), ExportStatus::done,
                Backoff.every(Duration.ofSeconds(1)).attempts(10));

        // 3. a loop that runs at least once. In plain Java, do-while:
        List<String> waves = new ArrayList<>();
        do {
            waves.add(tasks.pickWave(waves.size() + 1));
        } while (waves.size() < 2);
        // ... and with doWhile, which names the loop "pick_waves" in the diagram:
        List<String> moreWaves = new ArrayList<>();
        doWhile("pick_waves", () -> moreWaves.add(tasks.pickWave(moreWaves.size() + 1)), () -> moreWaves.size() < 2);

        return new Report(packed, packedByServer, checks, status.check(), waves, moreWaves);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Loops.class).tasks(new Tasks()).start();

            String id = "batch-" + System.currentTimeMillis();
            WorkflowHandle<Report> run = conductor.start(Loops::run, new Batch(id, List.of("mug", "tea", "plate")));
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofMinutes(1)));
        }
        client.shutdown();
    }
}
