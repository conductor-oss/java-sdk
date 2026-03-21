package inlinetasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates workflow.json structure and INLINE task definitions.
 * Since all tasks are INLINE (server-side), there are no workers to unit-test.
 */
class WorkflowJsonTest {

    private static WorkflowDef workflowDef;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void loadWorkflow() throws Exception {
        try (InputStream is = WorkflowJsonTest.class.getClassLoader().getResourceAsStream("workflow.json")) {
            assertNotNull(is, "workflow.json must exist on classpath");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            workflowDef = mapper.readValue(json, WorkflowDef.class);
        }
    }

    @Test
    void workflowNameAndVersion() {
        assertEquals("inline_tasks_demo", workflowDef.getName());
        assertEquals(1, workflowDef.getVersion());
    }

    @Test
    void schemaVersionIsTwo() {
        assertEquals(2, workflowDef.getSchemaVersion());
    }

    @Test
    void hasFourTasks() {
        List<WorkflowTask> tasks = workflowDef.getTasks();
        assertEquals(4, tasks.size(), "Workflow should have exactly 4 INLINE tasks");
    }

    @Test
    void allTasksAreInlineType() {
        for (WorkflowTask task : workflowDef.getTasks()) {
            assertEquals("INLINE", task.getType(),
                    "Task '" + task.getName() + "' should be INLINE type");
        }
    }

    @Test
    void allTasksUseGraalJsEvaluator() {
        for (WorkflowTask task : workflowDef.getTasks()) {
            Map<String, Object> inputParams = task.getInputParameters();
            assertEquals("graaljs", inputParams.get("evaluatorType"),
                    "Task '" + task.getName() + "' should use graaljs evaluator");
        }
    }

    @Test
    void allTasksHaveExpressions() {
        for (WorkflowTask task : workflowDef.getTasks()) {
            Map<String, Object> inputParams = task.getInputParameters();
            String expression = (String) inputParams.get("expression");
            assertNotNull(expression, "Task '" + task.getName() + "' must have an expression");
            assertFalse(expression.isBlank(), "Task '" + task.getName() + "' expression must not be blank");
        }
    }

    @Test
    void taskNamesAreCorrect() {
        List<String> expectedNames = List.of(
                "math_aggregation",
                "string_manipulation",
                "conditional_logic",
                "build_response"
        );
        List<String> actualNames = workflowDef.getTasks().stream()
                .map(WorkflowTask::getName)
                .toList();
        assertEquals(expectedNames, actualNames);
    }

    @Test
    void taskReferenceNamesAreCorrect() {
        List<String> expectedRefNames = List.of(
                "math_aggregation_ref",
                "string_manipulation_ref",
                "conditional_logic_ref",
                "build_response_ref"
        );
        List<String> actualRefNames = workflowDef.getTasks().stream()
                .map(WorkflowTask::getTaskReferenceName)
                .toList();
        assertEquals(expectedRefNames, actualRefNames);
    }

    @Test
    void mathTaskReceivesNumbersInput() {
        WorkflowTask mathTask = workflowDef.getTasks().get(0);
        Map<String, Object> inputParams = mathTask.getInputParameters();
        assertEquals("${workflow.input.numbers}", inputParams.get("numbers"));
    }

    @Test
    void stringTaskReceivesTextInput() {
        WorkflowTask stringTask = workflowDef.getTasks().get(1);
        Map<String, Object> inputParams = stringTask.getInputParameters();
        assertEquals("${workflow.input.text}", inputParams.get("text"));
    }

    @Test
    void conditionalTaskReferencesMathOutput() {
        WorkflowTask condTask = workflowDef.getTasks().get(2);
        Map<String, Object> inputParams = condTask.getInputParameters();
        assertEquals("${math_aggregation_ref.output.result.average}", inputParams.get("average"));
        assertEquals("${workflow.input.config}", inputParams.get("config"));
    }

    @Test
    void buildResponseReferencesAllPriorTasks() {
        WorkflowTask buildTask = workflowDef.getTasks().get(3);
        Map<String, Object> inputParams = buildTask.getInputParameters();

        // References to math_aggregation_ref
        assertEquals("${math_aggregation_ref.output.result.sum}", inputParams.get("sum"));
        assertEquals("${math_aggregation_ref.output.result.average}", inputParams.get("average"));
        assertEquals("${math_aggregation_ref.output.result.min}", inputParams.get("min"));
        assertEquals("${math_aggregation_ref.output.result.max}", inputParams.get("max"));
        assertEquals("${math_aggregation_ref.output.result.count}", inputParams.get("count"));

        // References to string_manipulation_ref
        assertEquals("${string_manipulation_ref.output.result.uppercase}", inputParams.get("uppercase"));
        assertEquals("${string_manipulation_ref.output.result.wordCount}", inputParams.get("wordCount"));
        assertEquals("${string_manipulation_ref.output.result.slug}", inputParams.get("slug"));
        assertEquals("${string_manipulation_ref.output.result.reversed}", inputParams.get("reversed"));

        // References to conditional_logic_ref
        assertEquals("${conditional_logic_ref.output.result.tier}", inputParams.get("tier"));
        assertEquals("${conditional_logic_ref.output.result.isHighPerformer}", inputParams.get("isHighPerformer"));
        assertEquals("${conditional_logic_ref.output.result.isEnabled}", inputParams.get("isEnabled"));
    }

    @Test
    void workflowOutputParametersAreDefined() {
        Map<String, Object> outputParams = workflowDef.getOutputParameters();
        assertNotNull(outputParams);
        assertEquals("${build_response_ref.output.result.math}", outputParams.get("math"));
        assertEquals("${build_response_ref.output.result.text}", outputParams.get("text"));
        assertEquals("${build_response_ref.output.result.classification}", outputParams.get("classification"));
        assertEquals("${build_response_ref.output.result.summary}", outputParams.get("summary"));
    }

    @Test
    void workflowHasOwnerEmail() {
        assertEquals("examples@orkes.io", workflowDef.getOwnerEmail());
    }

    @Test
    void workflowHasTimeout() {
        assertTrue(workflowDef.getTimeoutSeconds() > 0, "Workflow must have a positive timeout");
    }

    @Test
    void taskDefsJsonIsEmptyArray() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("task-defs.json")) {
            assertNotNull(is, "task-defs.json must exist on classpath");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Object parsed = mapper.readValue(json, List.class);
            assertTrue(((List<?>) parsed).isEmpty(), "task-defs.json should be an empty array (no workers needed)");
        }
    }

    @Test
    void inputParametersIncludeExpectedFields() {
        List<String> inputParams = workflowDef.getInputParameters();
        assertTrue(inputParams.contains("numbers"), "Workflow should accept 'numbers' input");
        assertTrue(inputParams.contains("text"), "Workflow should accept 'text' input");
        assertTrue(inputParams.contains("config"), "Workflow should accept 'config' input");
    }
}
