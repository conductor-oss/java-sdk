package jqtransformadvanced;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the workflow.json and task-defs.json are valid and correctly structured.
 *
 * Since this example has NO workers (all tasks are JSON_JQ_TRANSFORM system tasks),
 * we validate the workflow definition itself rather than testing worker logic.
 */
class WorkflowDefinitionTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonNode workflowJson;
    private static JsonNode taskDefsJson;

    @BeforeAll
    static void loadResources() throws Exception {
        try (InputStream wfStream = WorkflowDefinitionTest.class.getClassLoader()
                .getResourceAsStream("workflow.json")) {
            assertNotNull(wfStream, "workflow.json must exist in resources");
            String json = new String(wfStream.readAllBytes(), StandardCharsets.UTF_8);
            workflowJson = mapper.readTree(json);
        }

        try (InputStream tdStream = WorkflowDefinitionTest.class.getClassLoader()
                .getResourceAsStream("task-defs.json")) {
            assertNotNull(tdStream, "task-defs.json must exist in resources");
            String json = new String(tdStream.readAllBytes(), StandardCharsets.UTF_8);
            taskDefsJson = mapper.readTree(json);
        }
    }

    @Test
    void workflowJsonIsValidJson() {
        assertNotNull(workflowJson);
        assertTrue(workflowJson.isObject(), "workflow.json must be a JSON object");
    }

    @Test
    void workflowHasCorrectName() {
        assertEquals("jq_advanced_demo", workflowJson.get("name").asText());
    }

    @Test
    void workflowHasVersion() {
        assertEquals(1, workflowJson.get("version").asInt());
    }

    @Test
    void workflowHasSchemaVersion2() {
        assertEquals(2, workflowJson.get("schemaVersion").asInt());
    }

    @Test
    void workflowAcceptsOrdersInput() {
        JsonNode inputParams = workflowJson.get("inputParameters");
        assertNotNull(inputParams);
        assertTrue(inputParams.isArray());
        boolean hasOrders = false;
        for (JsonNode param : inputParams) {
            if ("orders".equals(param.asText())) {
                hasOrders = true;
                break;
            }
        }
        assertTrue(hasOrders, "Workflow must accept 'orders' as an input parameter");
    }

    @Test
    void workflowHasThreeTasks() {
        JsonNode tasks = workflowJson.get("tasks");
        assertNotNull(tasks);
        assertTrue(tasks.isArray());
        assertEquals(3, tasks.size(), "Workflow must have exactly 3 tasks");
    }

    @Test
    void firstTaskIsJqFlatten() {
        JsonNode task = workflowJson.get("tasks").get(0);
        assertEquals("jq_flatten", task.get("name").asText());
        assertEquals("jq_flatten_ref", task.get("taskReferenceName").asText());
        assertEquals("JSON_JQ_TRANSFORM", task.get("type").asText());
    }

    @Test
    void secondTaskIsJqAggregate() {
        JsonNode task = workflowJson.get("tasks").get(1);
        assertEquals("jq_aggregate", task.get("name").asText());
        assertEquals("jq_aggregate_ref", task.get("taskReferenceName").asText());
        assertEquals("JSON_JQ_TRANSFORM", task.get("type").asText());
    }

    @Test
    void thirdTaskIsJqClassify() {
        JsonNode task = workflowJson.get("tasks").get(2);
        assertEquals("jq_classify", task.get("name").asText());
        assertEquals("jq_classify_ref", task.get("taskReferenceName").asText());
        assertEquals("JSON_JQ_TRANSFORM", task.get("type").asText());
    }

    @Test
    void allTasksAreJsonJqTransform() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            assertEquals("JSON_JQ_TRANSFORM", task.get("type").asText(),
                    "Task '" + task.get("name").asText() + "' must be JSON_JQ_TRANSFORM");
        }
    }

    @Test
    void noSimpleTasksExist() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            assertNotEquals("SIMPLE", task.get("type").asText(),
                    "JQ transform example must not contain SIMPLE tasks (which require workers)");
        }
    }

    @Test
    void allTasksHaveQueryExpression() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            JsonNode inputParams = task.get("inputParameters");
            assertNotNull(inputParams.get("queryExpression"),
                    "JSON_JQ_TRANSFORM task '" + task.get("name").asText()
                            + "' must have a queryExpression");
            assertFalse(inputParams.get("queryExpression").asText().isBlank(),
                    "queryExpression must not be blank for task '" + task.get("name").asText() + "'");
        }
    }

    @Test
    void flattenExpressionHandlesNestedOrders() {
        String expr = workflowJson.get("tasks").get(0)
                .get("inputParameters").get("queryExpression").asText();
        assertTrue(expr.contains(".orders[]"), "Flatten expression must iterate over orders");
        assertTrue(expr.contains("orderId"), "Flatten expression must produce orderId");
        assertTrue(expr.contains("customerName"), "Flatten expression must produce customerName");
        assertTrue(expr.contains("orderTotal"), "Flatten expression must calculate orderTotal");
    }

    @Test
    void aggregateExpressionGroupsByCustomer() {
        String expr = workflowJson.get("tasks").get(1)
                .get("inputParameters").get("queryExpression").asText();
        assertTrue(expr.contains("group_by"), "Aggregate expression must use group_by");
        assertTrue(expr.contains("customerName"), "Aggregate expression must reference customerName");
        assertTrue(expr.contains("totalSpent"), "Aggregate expression must compute totalSpent");
        assertTrue(expr.contains("orderCount"), "Aggregate expression must compute orderCount");
    }

    @Test
    void classifyExpressionHasTiers() {
        String expr = workflowJson.get("tasks").get(2)
                .get("inputParameters").get("queryExpression").asText();
        assertTrue(expr.contains("gold"), "Classify expression must include gold tier");
        assertTrue(expr.contains("silver"), "Classify expression must include silver tier");
        assertTrue(expr.contains("bronze"), "Classify expression must include bronze tier");
        assertTrue(expr.contains("tier"), "Classify expression must assign a tier field");
    }

    @Test
    void flattenTaskReceivesWorkflowInput() {
        JsonNode inputParams = workflowJson.get("tasks").get(0).get("inputParameters");
        assertEquals("${workflow.input.orders}", inputParams.get("orders").asText(),
                "jq_flatten must receive orders from workflow input");
    }

    @Test
    void aggregateTaskChainsFlattenOutput() {
        JsonNode inputParams = workflowJson.get("tasks").get(1).get("inputParameters");
        String flattenedRef = inputParams.get("flattenedOrders").asText();
        assertTrue(flattenedRef.contains("jq_flatten_ref"),
                "jq_aggregate must reference jq_flatten_ref output");
    }

    @Test
    void classifyTaskChainsAggregateOutput() {
        JsonNode inputParams = workflowJson.get("tasks").get(2).get("inputParameters");
        String summaryRef = inputParams.get("customerSummary").asText();
        assertTrue(summaryRef.contains("jq_aggregate_ref"),
                "jq_classify must reference jq_aggregate_ref output");
    }

    @Test
    void workflowHasOutputParameters() {
        JsonNode outputParams = workflowJson.get("outputParameters");
        assertNotNull(outputParams);
        assertTrue(outputParams.has("flattenedOrders"),
                "Workflow output must include 'flattenedOrders'");
        assertTrue(outputParams.has("customerSummary"),
                "Workflow output must include 'customerSummary'");
        assertTrue(outputParams.has("tieredCustomers"),
                "Workflow output must include 'tieredCustomers'");
    }

    @Test
    void workflowHasTimeoutAndOwner() {
        assertTrue(workflowJson.has("timeoutSeconds"));
        assertTrue(workflowJson.get("timeoutSeconds").asInt() > 0);
        assertEquals("examples@orkes.io", workflowJson.get("ownerEmail").asText());
    }

    @Test
    void taskDefsJsonIsEmptyArray() {
        assertNotNull(taskDefsJson);
        assertTrue(taskDefsJson.isArray(), "task-defs.json must be a JSON array");
        assertEquals(0, taskDefsJson.size(),
                "task-defs.json must be empty — system tasks don't need task definitions");
    }

    @Test
    void workflowCanBeDeserializedToWorkflowDef() throws Exception {
        try (InputStream is = WorkflowDefinitionTest.class.getClassLoader()
                .getResourceAsStream("workflow.json")) {
            assertNotNull(is);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var workflowDef = mapper.readValue(json,
                    com.netflix.conductor.common.metadata.workflow.WorkflowDef.class);
            assertEquals("jq_advanced_demo", workflowDef.getName());
            assertEquals(1, workflowDef.getVersion());
            assertEquals(3, workflowDef.getTasks().size());
        }
    }

    @Test
    void sampleOrdersAreWellFormed() {
        List<Map<String, Object>> orders = JqTransformAdvancedExample.buildSampleOrders();
        assertNotNull(orders);
        assertTrue(orders.size() >= 3, "Must have at least 3 sample orders");
        for (Map<String, Object> order : orders) {
            assertNotNull(order.get("id"), "Each order must have an id");
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) order.get("customer");
            assertNotNull(customer, "Each order must have a customer");
            assertNotNull(customer.get("name"), "Customer must have a name");
            assertNotNull(customer.get("email"), "Customer must have an email");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            assertNotNull(items, "Each order must have items");
            assertFalse(items.isEmpty(), "Items list must not be empty");
            for (Map<String, Object> item : items) {
                assertNotNull(item.get("name"), "Each item must have a name");
                assertNotNull(item.get("price"), "Each item must have a price");
                assertNotNull(item.get("qty"), "Each item must have a qty");
            }
        }
    }

    @Test
    void sampleOrdersHaveRepeatingCustomerForAggregation() {
        List<Map<String, Object>> orders = JqTransformAdvancedExample.buildSampleOrders();
        long distinctCustomers = orders.stream()
                .map(o -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> c = (Map<String, Object>) o.get("customer");
                    return c.get("name");
                })
                .distinct()
                .count();
        assertTrue(distinctCustomers < orders.size(),
                "Sample orders must have at least one repeating customer to test aggregation");
    }
}
