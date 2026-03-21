package systemtasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the workflow.json and task-defs.json are valid and correctly structured.
 *
 * Since this example has NO workers (all tasks are system tasks), we validate
 * the workflow definition itself rather than testing worker logic.
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
        assertEquals("system_tasks_demo", workflowJson.get("name").asText());
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
    void workflowAcceptsUserIdInput() {
        JsonNode inputParams = workflowJson.get("inputParameters");
        assertNotNull(inputParams);
        assertTrue(inputParams.isArray());
        boolean hasUserId = false;
        for (JsonNode param : inputParams) {
            if ("userId".equals(param.asText())) {
                hasUserId = true;
                break;
            }
        }
        assertTrue(hasUserId, "Workflow must accept 'userId' as an input parameter");
    }

    @Test
    void workflowHasThreeTasks() {
        JsonNode tasks = workflowJson.get("tasks");
        assertNotNull(tasks);
        assertTrue(tasks.isArray());
        assertEquals(3, tasks.size(), "Workflow must have exactly 3 tasks");
    }

    @Test
    void firstTaskIsInlineLookupUser() {
        JsonNode task = workflowJson.get("tasks").get(0);
        assertEquals("lookup_user", task.get("name").asText());
        assertEquals("lookup_user_ref", task.get("taskReferenceName").asText());
        assertEquals("INLINE", task.get("type").asText());
    }

    @Test
    void secondTaskIsInlineCalculateBonus() {
        JsonNode task = workflowJson.get("tasks").get(1);
        assertEquals("calculate_bonus", task.get("name").asText());
        assertEquals("calculate_bonus_ref", task.get("taskReferenceName").asText());
        assertEquals("INLINE", task.get("type").asText());
    }

    @Test
    void thirdTaskIsJsonJqTransform() {
        JsonNode task = workflowJson.get("tasks").get(2);
        assertEquals("format_output", task.get("name").asText());
        assertEquals("format_output_ref", task.get("taskReferenceName").asText());
        assertEquals("JSON_JQ_TRANSFORM", task.get("type").asText());
    }

    @Test
    void allTasksAreSystemTasks() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            String type = task.get("type").asText();
            assertTrue(
                    "INLINE".equals(type) || "JSON_JQ_TRANSFORM".equals(type),
                    "Task '" + task.get("name").asText() + "' must be a system task, got: " + type
            );
        }
    }

    @Test
    void noSimpleTasksExist() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            assertNotEquals("SIMPLE", task.get("type").asText(),
                    "System tasks example must not contain SIMPLE tasks (which require workers)");
        }
    }

    @Test
    void inlineTasksUseGraalJsEvaluator() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            if ("INLINE".equals(task.get("type").asText())) {
                JsonNode inputParams = task.get("inputParameters");
                assertEquals("graaljs", inputParams.get("evaluatorType").asText(),
                        "INLINE task '" + task.get("name").asText() + "' must use graaljs evaluator");
                assertNotNull(inputParams.get("expression"),
                        "INLINE task '" + task.get("name").asText() + "' must have an expression");
            }
        }
    }

    @Test
    void jqTransformTaskHasQueryExpression() {
        JsonNode tasks = workflowJson.get("tasks");
        for (JsonNode task : tasks) {
            if ("JSON_JQ_TRANSFORM".equals(task.get("type").asText())) {
                JsonNode inputParams = task.get("inputParameters");
                assertNotNull(inputParams.get("queryExpression"),
                        "JSON_JQ_TRANSFORM task must have a queryExpression");
            }
        }
    }

    @Test
    void lookupUserExpressionContainsUserData() {
        JsonNode expression = workflowJson.get("tasks").get(0)
                .get("inputParameters").get("expression");
        String expr = expression.asText();
        assertTrue(expr.contains("user-1"), "Expression must contain user-1 data");
        assertTrue(expr.contains("user-2"), "Expression must contain user-2 data");
        assertTrue(expr.contains("user-3"), "Expression must contain user-3 data");
        assertTrue(expr.contains("Alice Johnson"), "Expression must contain Alice Johnson");
        assertTrue(expr.contains("Bob Smith"), "Expression must contain Bob Smith");
        assertTrue(expr.contains("Carol Williams"), "Expression must contain Carol Williams");
    }

    @Test
    void calculateBonusExpressionHasTiers() {
        JsonNode expression = workflowJson.get("tasks").get(1)
                .get("inputParameters").get("expression");
        String expr = expression.asText();
        assertTrue(expr.contains("bonusPercent"), "Expression must calculate bonusPercent");
        assertTrue(expr.contains("bonusAmount"), "Expression must calculate bonusAmount");
        assertTrue(expr.contains("Gold"), "Expression must have Gold tier");
        assertTrue(expr.contains("Silver"), "Expression must have Silver tier");
        assertTrue(expr.contains("Bronze"), "Expression must have Bronze tier");
    }

    @Test
    void workflowHasOutputParameters() {
        JsonNode outputParams = workflowJson.get("outputParameters");
        assertNotNull(outputParams);
        assertTrue(outputParams.has("summary"), "Workflow output must include 'summary'");
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
            assertEquals("system_tasks_demo", workflowDef.getName());
            assertEquals(1, workflowDef.getVersion());
            assertEquals(3, workflowDef.getTasks().size());
        }
    }
}
