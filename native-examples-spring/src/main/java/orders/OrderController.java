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
package orders;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import orders.Orders.OrderRequest;
import orders.Orders.OrderResult;

/** Starts executions and reads them back, through the {@code Conductor} bean. */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final Conductor conductor;

    public OrderController(Conductor conductor) {
        this.conductor = conductor;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody OrderRequest order) {
        WorkflowHandle<OrderResult> run = conductor.start(OrderWorkflow::process, order);
        return view(run);
    }

    @GetMapping("/{workflowId}")
    public Map<String, Object> get(@PathVariable String workflowId) {
        return view(conductor.attach(OrderWorkflow::process, workflowId));
    }

    private static Map<String, Object> view(WorkflowHandle<OrderResult> run) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("workflowId", run.workflowId());
        view.put("url", run.url());
        String status = run.status();
        view.put("status", status);
        if ("COMPLETED".equals(status)) {
            view.put("result", run.result(Duration.ofSeconds(5)));
        }
        return view;
    }
}
