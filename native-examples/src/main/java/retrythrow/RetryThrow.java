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
package retrythrow;

import java.time.Duration;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.conductoross.conductor.sdk.Conductor.async;

/**
 * A retry after the code threw runs it again. What the code drew in the continuation that threw
 * (an if it went past, a lane it started) is drawn again by the run after the retry: the retry
 * must not leave it behind, or the second drawing reuses its names. On the same code, the run
 * fails again where it threw.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" retrythrow.RetryThrow
 * </pre>
 */
public class RetryThrow {

    public static class Tasks {
        @WorkerTask("rt_a")
        public String a(@InputParam("x") String x) {
            return "a(" + x + ")";
        }

        @WorkerTask("rt_b")
        public String b(@InputParam("x") String x) {
            return "b(" + x + ")";
        }
    }

    private final Tasks tasks;

    public RetryThrow(Tasks tasks) {
        this.tasks = tasks;
    }

    /** Throws in the continuation that went past an if: the if is drawn in the same fragment. */
    @Workflow(name = "rt_switch_then_throw")
    public String switchThenThrow(@InputParam("x") String x) {
        String r = tasks.a(x);
        if (x.equals("never")) {
            r = tasks.b(r);
        }
        if (!x.isEmpty()) {
            throw new IllegalStateException("boom");
        }
        return r;
    }

    /** Throws in the continuation that started a lane: the fork is drawn in the same fragment. */
    @Workflow(name = "rt_async_then_throw")
    public String asyncThenThrow(@InputParam("x") String x) {
        String r = tasks.a(x);
        async(() -> tasks.b(x));
        if (!x.isEmpty()) {
            throw new IllegalStateException("boom");
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(RetryThrow.class).tasks(new Tasks()).start();
        int bad = 0;
        bad += check(conductor, "switch then throw", conductor.start(RetryThrow::switchThenThrow, "1"));
        bad += check(conductor, "async then throw", conductor.start(RetryThrow::asyncThenThrow, "1"));
        System.out.println("RETRYTHROW " + (bad == 0 ? "pass" : "FAIL " + bad));
        conductor.close();
        client.shutdown();
        System.exit(bad == 0 ? 0 : 1);
    }

    /** Fails, is retried, and must fail again where the code threw: 0 when it does. */
    private static int check(Conductor conductor, String name, WorkflowHandle<String> run) throws Exception {
        String first = ended(conductor, run.workflowId());
        post(conductor, "/workflow/" + run.workflowId() + "/retry");
        Thread.sleep(500);
        String second = ended(conductor, run.workflowId());
        boolean ok = first.contains("boom") && second.contains("boom");
        System.out.println((ok ? "ok   " : "BAD  ") + name + " http://localhost:5173/execution/" + run.workflowId()
                + "\n      first:  " + first + "\n      after retry: " + second);
        return ok ? 0 : 1;
    }

    /** Waits for the run to end; its status and reason. */
    private static String ended(Conductor conductor, String id) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> wf = conductor.client().execute(
                    ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + id).build(),
                    new TypeReference<Map<String, Object>>() {}).getData();
            String status = String.valueOf(wf.get("status"));
            if (!status.equals("RUNNING")) {
                return status + " " + wf.get("reasonForIncompletion");
            }
            Thread.sleep(200);
        }
        return "STILL RUNNING";
    }

    private static void post(Conductor conductor, String path) {
        conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.POST).path(path).build());
    }
}
