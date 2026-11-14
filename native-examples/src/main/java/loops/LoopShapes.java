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

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * The three Java loop shapes in workflow code: {@code while (...) {...}}, {@code do {...} while (...)}
 * and {@code for (;;) {...}} with a break. None becomes a server loop: each call is a task of its
 * own, and the condition runs only in the code.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" loops.LoopShapes [worker]
 * </pre>
 */
public class LoopShapes {

    public static class Tasks {
        @WorkerTask("loop_step")
        public String step(@InputParam("label") String label) {
            return "did " + label;
        }

        @WorkerTask("loop_ready")
        public boolean ready(@InputParam("round") int round) {
            return round >= 2; // ready on the second round
        }
    }

    private final Tasks tasks;

    public LoopShapes(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "loop_shapes")
    public String run(@InputParam("n") int n) {
        // while: a plain Java condition, checked before each round
        int i = 0;
        while (i < n) {
            tasks.step("while " + i);
            i++;
        }

        // do-while: the condition calls a task, checked after each round
        int round = 0;
        do {
            round++;
            tasks.step("do " + round);
        } while (!tasks.ready(round));

        // for (;;): no condition at all, the body breaks out
        int k = 0;
        for (;;) {
            tasks.step("for " + k);
            k++;
            if (k == 3) {
                break;
            }
        }
        return i + " while / " + round + " do / " + k + " for";
    }

    /** {@code worker}: host the code and its tasks only, for runs started on the server. */
    public static void main(String[] args) throws InterruptedException {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(LoopShapes.class).tasks(new Tasks()).start();
        if (args.length > 0 && args[0].equals("worker")) {
            System.out.println("loop_shapes worker started");
            Thread.currentThread().join();
        }
        WorkflowHandle<String> run = conductor.start(LoopShapes::run, 2);
        System.out.println("execution: " + run.workflowId());
        System.out.println("result:    " + run.result(Duration.ofSeconds(60)));
        conductor.close();
        client.shutdown();
        System.exit(0);
    }
}
