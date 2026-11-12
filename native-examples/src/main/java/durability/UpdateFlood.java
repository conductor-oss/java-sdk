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
package durability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.run.Workflow;

import durability.LedgerModel.LedgerInput;
import durability.LedgerModel.LedgerResult;

/**
 * Many concurrent updates and queries on one execution. Checks that every update is applied once
 * and in a single order (the returned counter values are exactly 1..N), that queries keep working,
 * that the stored replies stay bounded, and that the execution still completes.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=... java durability.UpdateFlood [updates=1000] [threads=20]
 * </pre>
 */
public final class UpdateFlood {

    private UpdateFlood() {}

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        int updates = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        WorkflowClient workflows = new WorkflowClient(conductor.client());
        conductor.host().workflows(LedgerWorkflow.class, LedgerChild.class).tasks(new LedgerTasks()).start();

        String key = "flood-" + Long.toString(System.currentTimeMillis(), 36);
        WorkflowHandle<LedgerResult> run =
                conductor.workflow(LedgerWorkflow::run).idempotencyKey(key).start(new LedgerInput(key, 1));
        System.out.println("execution " + run.workflowId() + ": " + updates + " updates from " + threads + " threads");

        Set<Integer> returned = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicInteger maxReplies = new AtomicInteger();
        AtomicInteger queries = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads + 2);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger next = new AtomicInteger();
        long started = System.currentTimeMillis();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                while (next.getAndIncrement() < updates) {
                    try {
                        int value = run.update(LedgerWorkflow::bump, "upd-" + next.get());
                        if (!returned.add(value)) {
                            duplicates.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                        System.out.println("update failed: " + e);
                    }
                }
            }));
        }
        // meanwhile: queries keep answering, and the reply store is sampled
        Future<?> watcher = pool.submit(() -> {
            while (returned.size() + failures.get() < updates) {
                maxReplies.accumulateAndGet(replies(workflows, run.workflowId()), Math::max);
                run.query(LedgerWorkflow::depositsSoFar);
                queries.incrementAndGet();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        for (Future<?> f : futures) {
            f.get();
        }
        watcher.get();
        long millis = System.currentTimeMillis() - started;

        run.signal(LedgerWorkflow::close);
        LedgerResult result = run.result(Duration.ofMinutes(10));
        int finalReplies = replies(workflows, run.workflowId());

        Set<Integer> expected = new TreeSet<>();
        for (int i = 1; i <= updates; i++) {
            expected.add(i);
        }
        List<String> problems = new ArrayList<>();
        if (failures.get() > 0) {
            problems.add(failures.get() + " updates failed");
        }
        if (duplicates.get() > 0) {
            problems.add(duplicates.get() + " updates returned a value another update had returned");
        }
        if (!new TreeSet<>(returned).equals(expected)) {
            problems.add("returned values are not exactly 1.." + updates + " (" + returned.size() + " distinct)");
        }
        if (result.bumps() != updates) {
            problems.add("the execution applied " + result.bumps() + " updates, expected " + updates);
        }
        if (maxReplies.get() > 500 || finalReplies > 500) {
            problems.add("stored replies grew to " + Math.max(maxReplies.get(), finalReplies));
        }
        System.out.printf("updates: %d in %.1f s (%.1f/s); queries during the flood: %d%n",
                updates, millis / 1000.0, updates * 1000.0 / millis, queries.get());
        System.out.printf("stored replies: max %d while flooding, %d at the end (limit 500)%n", maxReplies.get(), finalReplies);
        System.out.println(problems.isEmpty() ? "UPDATE FLOOD PASSED" : "UPDATE FLOOD FAILED: " + problems);
        pool.shutdownNow();
        conductor.close();
        client.shutdown();
        System.exit(problems.isEmpty() ? 0 : 1);
    }

    /** How many replies to updates and queries the execution keeps (pruned by the handler lane). */
    private static int replies(WorkflowClient workflows, String workflowId) {
        Workflow wf = workflows.getWorkflow(workflowId, false);
        return wf.getNativeState() == null || wf.getNativeState().getReplies() == null
                ? 0
                : wf.getNativeState().getReplies().size();
    }
}
