/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.sdk.examples.shipment;

import java.time.Duration;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.sdk.examples.util.ClientUtil;

public class Main {

    public static void main(String[] args) {
        ConductorClient client = ClientUtil.getClient();
        Conductor conductor = Conductor.of(client);

        // Host both workflows: the shipment, and the order flow it runs for each line item
        conductor.host()
                .workflows(ShipmentWorkflow.class, OrderFlow.class)
                .tasks(new ShipmentWorkers())
                .start();

        WorkflowHandle<List<String>> run =
                conductor.start(ShipmentWorkflow::ship, new Shipment("userA", "order123"));
        System.out.println("Workflow Id: " + run.workflowId());
        try {
            System.out.println("Shipping labels: " + run.result(Duration.ofMinutes(1)));
        } catch (RuntimeException e) {
            System.out.println("Workflow did not complete: " + e.getMessage());
        } finally {
            conductor.close();
            client.shutdown();
        }
        System.out.println("Done");
    }
}
