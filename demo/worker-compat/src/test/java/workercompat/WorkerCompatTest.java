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
package workercompat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorker;
import com.netflix.conductor.sdk.workflow.task.InputParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Ordinary worker code. Every test asserts what the Jackson 2 SDK (6.0.2) does, so it passes on
 * 6.0.2 and shows what changes for the same code on the Jackson 3 SDK.
 *
 * <p>Inputs are checked on the object the worker method receives. Outputs are checked on the JSON
 * that TaskClient.updateTask() actually sends, captured by a fake server.
 *
 * <p>Customer and Shipment carry Jackson 2 annotations and come from src/userCode/{jackson2,migrated},
 * picked with -PuserCode. Tier is in Tier.java.
 */
class WorkerCompatTest {

    // ---- Worker types, as a user would write them ----

    public static class Order {
        public Tier tier;
        public final String region = null;
        private final List<String> items = new ArrayList<>();

        public List<String> getItems() {
            return items;
        }
    }

    public static class Workers {
        Order order;
        Customer customer;

        public void takeOrder(@InputParam("order") Order order) {
            this.order = order;
        }

        public void takeCustomer(@InputParam("customer") Customer customer) {
            this.customer = customer;
        }

        public Shipment ship() {
            return new Shipment();
        }

        // A worker that returns a REST response it parsed with its own Jackson 2.
        public Map<String, Object> callApi() throws Exception {
            Map<String, Object> output = new HashMap<>();
            output.put("response", new ObjectMapper().readTree("{\"status\":\"ok\",\"count\":1}"));
            return output;
        }
    }

    // ---- Fake Conductor server that records task updates ----

    private HttpServer server;
    private final List<String> requestBodies = new ArrayList<>();
    private final Workers workers = new Workers();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    // ---- Issue 2: inputs ----

    @Test
    @DisplayName("Issue 2 input: enum with custom toString() binds from \"GOLD\"")
    void enumBindsFromName() {
        run("takeOrder", Map.of("order", Map.of("tier", "GOLD")));
        assertThat(workers.order.tier).isEqualTo(Tier.GOLD);
    }

    @Test
    @DisplayName("Issue 2 input: getter-only list is filled")
    void getterOnlyListIsFilled() {
        run("takeOrder", Map.of("order", Map.of("items", List.of("a", "b"))));
        assertThat(workers.order.getItems()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Issue 2 input: final field is set")
    void finalFieldIsSet() {
        run("takeOrder", Map.of("order", Map.of("region", "eu")));
        assertThat(workers.order.region).isEqualTo("eu");
    }

    @Test
    @DisplayName("Issue 2 input: Jackson 2 @JsonNaming is applied (user code change needed on Jackson 3)")
    void jackson2JsonNamingIsApplied() {
        run("takeCustomer", Map.of("customer", Map.of("customer_id", "c1")));
        assertThat(workers.customer.customerId).isEqualTo("c1");
    }

    // ---- Issue 2: outputs ----

    @Test
    @DisplayName("Issue 2 output: enum is sent by name (\"GOLD\")")
    void enumIsSentByName() {
        assertThat(sentOutput("ship").get("tier").asText()).isEqualTo("GOLD");
    }

    @Test
    @DisplayName("Issue 2 output: Duration is sent as a number of seconds (90.0)")
    void durationIsSentAsNumber() {
        JsonNode timeout = sentOutput("ship").get("timeout");
        assertThat(timeout.isNumber()).as("timeout sent as %s", timeout).isTrue();
        assertThat(timeout.asDouble()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Issue 2 output: Jackson 2 @JsonSerialize is applied (user code change needed on Jackson 3)")
    void jackson2JsonSerializeIsApplied() {
        JsonNode trackingId = sentOutput("ship").get("trackingId");
        assertThat(trackingId.isTextual()).as("trackingId sent as %s", trackingId).isTrue();
    }

    // ---- Issue 3 ----

    @Test
    @DisplayName("Issue 3 output: Jackson 2 JsonNode is sent as its JSON")
    void jackson2JsonNodeIsSentAsJson() throws Exception {
        assertThat(sentOutput("callApi").get("response"))
                .isEqualTo(new ObjectMapper().readTree("{\"status\":\"ok\",\"count\":1}"));
    }

    // ---- Helpers ----

    private TaskResult run(String method, Map<String, Object> input) {
        try {
            var workerMethod = java.util.Arrays.stream(Workers.class.getMethods())
                    .filter(m -> m.getName().equals(method))
                    .findFirst()
                    .orElseThrow();
            Task task = new Task();
            task.setTaskId("task-1");
            task.setWorkflowInstanceId("wf-1");
            task.setStatus(Task.Status.IN_PROGRESS);
            task.setInputData(new HashMap<>(input));
            return new AnnotatedWorker(method, workerMethod, workers).execute(task);
        } catch (Exception e) {
            return fail("worker failed: " + e + (e.getCause() != null ? " / cause: " + e.getCause() : ""));
        }
    }

    /** Runs the worker, sends its result with TaskClient, and returns the outputData the server got. */
    private JsonNode sentOutput(String method) {
        TaskResult result = run(method, Map.of());
        ConductorClient client = ConductorClient.builder()
                .basePath("http://127.0.0.1:" + server.getAddress().getPort() + "/api")
                .build();
        new TaskClient(client).updateTask(result);
        try {
            return new ObjectMapper().readTree(requestBodies.get(requestBodies.size() - 1)).get("outputData");
        } catch (Exception e) {
            return fail("could not read request body: " + e);
        }
    }
}
