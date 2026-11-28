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
package examples.ex05_sub_workflows;

import java.util.List;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex05_sub_workflows.SubWorkflows.Done;
import examples.ex05_sub_workflows.SubWorkflows.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubWorkflowsTest {

    @Test
    void paysThenShipsEveryParcel() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory()
                .workflows(SubWorkflows.class, SubWorkflows.Payment.class, SubWorkflows.Shipping.class,
                        SubWorkflows.Receipt.class)
                .tasks(new SubWorkflows.Tasks());

        Done done = env.start(SubWorkflows::fulfil, new Order("A-1", 4250, List.of("box 1", "box 2"))).result();

        assertEquals(new Done("captured auth-A-1", List.of("shipped box 1", "shipped box 2")), done);
    }
}
