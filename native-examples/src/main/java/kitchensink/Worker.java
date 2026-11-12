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
package kitchensink;

import org.conductoross.conductor.sdk.Conductor;

import com.netflix.conductor.client.http.ConductorClient;

/**
 * Hosts the kitchensink's workflows and tasks and keeps running. Kill it and start it again while
 * executions are in flight: they continue where they stopped.
 */
public class Worker {

    static void host(Conductor conductor) {
        conductor.host()
                .buildId(System.getenv().getOrDefault("BUILD_ID", "dev"))
                .devMode(System.getenv("CONDUCTOR_DEV") != null)
                .workflows(KitchenSinkWorkflow.class, SubFlow.class)
                // the chaos run injects task failures through ChaosTasks
                .tasks(ChaosTasks.enabled() ? new ChaosTasks() : new KitchenSinkTasks())
                .start();
    }

    public static void main(String[] args) throws InterruptedException {
        host(Conductor.of(ConductorClient.fromEnvironment()));
        System.out.println("kitchensink worker started");
        Thread.currentThread().join();
    }
}
