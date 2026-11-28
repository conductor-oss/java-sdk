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

import org.conductoross.conductor.sdk.Workflow;

import kitchensink.KitchenSinkModel.OddEven;
import kitchensink.KitchenSinkModel.SubFlowInput;
import kitchensink.KitchenSinkModel.SubFlowResult;

/**
 * sub_flow_1 from rest/src/main/resources/kitchensink/sub_flow_1.json, as code.
 * KitchenSinkWorkflow calls it as a child with child(SubFlow::run, input), typed by the method.
 */
public class SubFlow {

    private final KitchenSinkTasks tasks;

    public SubFlow(KitchenSinkTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "sub_flow_1", version = 1, ownerEmail = "example@email.com")
    public SubFlowResult run(SubFlowInput input) {
        OddEven five = tasks.task5(input.mod(), input.oddEven());
        String six = tasks.task6(five.oddEven());
        return new SubFlowResult(five, six);
    }
}
