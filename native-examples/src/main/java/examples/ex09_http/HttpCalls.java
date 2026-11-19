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
package examples.ex09_http;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import com.fasterxml.jackson.databind.JsonNode;

import static org.conductoross.conductor.sdk.Conductor.http;
import static org.conductoross.conductor.sdk.Conductor.workflowId;

/**
 * 09 HTTP: calling a web API from a workflow, with no worker of your own.
 *
 * <p>{@code http(ref)} is Conductor's HTTP system task: the server makes the call, retries it and
 * records the request and the response on the execution.
 *
 * <pre>
 *   http(ref).get(url)                    also post, put and delete
 *     .header(name, value)                headers to send
 *     .body(object)                       a request body, sent as JSON
 *     .retries(count, delay)              retries when the call fails
 *     .as(Type.class) / .asJson()         the response body, typed or as JSON
 * </pre>
 *
 * A call that fails for good throws {@code TaskFailure}, as a task does (see 08).
 *
 * <p>Runs against the stub API in {@code native-examples/stubs/ai_stub.py}:
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex09_http
 * </pre>
 */
public class HttpCalls {

    /** The part of the API's order this workflow reads; other fields are ignored. */
    public record Order(String id, String customer, String status, double total, List<Item> items) {}

    public record Item(String sku, int qty) {}

    public record Request(String apiUrl, String orderId) {}

    @Workflow(name = "http_calls")
    public String ship(Request request) {
        // GET, the response as a record
        Order order = http("get_order").get(request.apiUrl() + "/orders/" + request.orderId())
                .retries(2, Duration.ofSeconds(1))
                .as(Order.class);

        // POST with a header and a JSON body, the response as JSON
        JsonNode shipment = http("create_shipment").post(request.apiUrl() + "/shipments")
                .header("X-Request-Id", workflowId())
                .body(Map.of("orderId", order.id(), "items", order.items()))
                .asJson();

        return order.customer() + "'s order " + order.id() + " (" + order.items().size() + " items) ships as "
                + shipment.path("shipmentId").asText() + " with " + shipment.path("carrier").asText();
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        String apiUrl = System.getenv().getOrDefault("STUB_URL", "http://localhost:8765");
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(HttpCalls.class).start();

            WorkflowHandle<String> run = conductor.start(HttpCalls::ship, new Request(apiUrl, "1042"));
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
