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
package reuse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * A worker another team already runs, with no native workflow code in it: plain task definitions
 * and plain polling, one task in the default queue and one in the domain {@code team-a}.
 */
public class ExistingWorkers {

    private static Worker worker(String name, String prefix) {
        return new Worker() {
            @Override
            public String getTaskDefName() {
                return name;
            }

            @Override
            public TaskResult execute(Task task) {
                TaskResult result = new TaskResult(task);
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("result", prefix + ":" + task.getInputData().get("sku"));
                output.put("workerPid", ProcessHandle.current().pid());
                result.setOutputData(output);
                result.setStatus(TaskResult.Status.COMPLETED);
                return result;
            }
        };
    }

    private static TaskDef def(String name) {
        TaskDef def = new TaskDef(name, "owned by another team", "team-a@example.com", 7, 0, 60);
        return def;
    }

    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.builder().basePath(System.getenv("CONDUCTOR_SERVER_URL")).build();
        new MetadataClient(client).registerTaskDefs(List.of(def("legacy_lookup"), def("legacy_enrich")));
        new TaskRunnerConfigurer.Builder(new TaskClient(client),
                List.of(worker("legacy_lookup", "looked-up"), worker("legacy_enrich", "enriched")))
                .withThreadCount(4)
                .withTaskToDomain(Map.of("legacy_enrich", "team-a"))
                .withTaskPollTimeout(100)
                .build()
                .init();
        System.out.println("existing workers polling, pid " + ProcessHandle.current().pid());
        Thread.currentThread().join();
    }
}
