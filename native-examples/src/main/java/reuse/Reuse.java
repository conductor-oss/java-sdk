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

import java.time.Duration;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.task;

/** A workflow that hosts no task of its own: both tasks run on {@link ExistingWorkers}. */
public class Reuse {

    // Conductor passes a stand-in whose calls schedule the tasks; the real Legacy runs on the workers.
    private final Legacy legacy;

    public Reuse(Legacy legacy) {
        this.legacy = legacy;
    }

    /** A typed view of a worker someone else runs; nothing here is hosted or implemented. */
    public interface Legacy {
        @WorkerTask("legacy_enrich")
        Map<String, Object> enrich(@InputParam("sku") String sku);
    }

    @Workflow(name = "reuse_existing_workers", description = "Calls workers another team runs")
    @SuppressWarnings("unchecked")
    public String run(String sku) {
        Map<String, Object> found = task("legacy_lookup").input("sku", sku).asMap(); // by name
        Map<String, Object> enriched = legacy.enrich(sku);                    // typed
        return found.get("result") + " / " + enriched.get("result");
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Reuse.class).start();
        WorkflowHandle<String> run = conductor.workflow(Reuse::run)
                .taskToDomain(Map.of("legacy_enrich", "team-a"))
                .start("sku-1");
        System.out.println("execution: " + run.url());
        System.out.println("result:    " + run.result(Duration.ofSeconds(60)));
        conductor.close();
        client.shutdown();
    }
}
