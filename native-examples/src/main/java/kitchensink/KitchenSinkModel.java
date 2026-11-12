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

import java.util.List;
import java.util.Map;

/**
 * Plain records. Jackson serializes them as task input and output, and as workflow input and output.
 * Nothing here knows about Conductor.
 */
public final class KitchenSinkModel {

    private KitchenSinkModel() {}

    // workflow input / output
    public record KitchenSinkInput(int mod, String oddEven, String task2Name, String mcpServer, String agentUrl) {
        public KitchenSinkInput(int mod, String oddEven, String task2Name) {
            this(mod, oddEven, task2Name, null, null);
        }
    }

    public record KitchenSinkResult(
            String runId,
            String oddEven,
            int count,
            List<String> statuses,
            List<String> workflowIds,
            boolean charged,
            String approvedBy,
            String callbackPayload,
            String note,
            boolean cancelled,
            List<String> labels,
            int toolCount,
            String weather,
            String agentReply,
            List<String> composed) {}

    // activity inputs / outputs
    public record Stats(int mod, String oddEven) {}

    public record OddEven(int mod, String oddEven) {}

    public record DynamicTask(String name, Map<String, Object> input) {}

    public record FanoutPlan(List<DynamicTask> tasks) {}

    public record Summary(int count) {}

    public record JobStatus(boolean done, String detail) {}

    public record Receipt(String id, int amount) {}

    // sub workflow
    public record SubFlowInput(int mod, String oddEven) {}

    public record SubFlowResult(OddEven task5, String task6) {}

    // human task form, external callback, signal payload
    public record Approval(String approver, boolean approved) {}

    public record Callback(String payload) {}

    public record CancelRequest(String reason) {}
}
