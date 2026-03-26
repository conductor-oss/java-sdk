package switchjavascript;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
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
 * Validates workflow.json structure and SWITCH task configuration
 * with JavaScript evaluator.
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
        assertEquals("switch_js_demo", workflowDef.getName());
        assertEquals(1, workflowDef.getVersion());
    }

    @Test
    void schemaVersionIsTwo() {
        assertEquals(2, workflowDef.getSchemaVersion());
    }

    @Test
    void hasTwoTopLevelTasks() {
        List<WorkflowTask> tasks = workflowDef.getTasks();
        assertEquals(2, tasks.size(), "Workflow should have SWITCH + finalize = 2 top-level tasks");
    }

    @Test
    void firstTaskIsSwitchType() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        assertEquals("SWITCH", switchTask.getType());
        assertEquals("route_order", switchTask.getName());
        assertEquals("route_order_ref", switchTask.getTaskReferenceName());
    }

    @Test
    void switchUsesJavascriptEvaluator() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        assertEquals("javascript", switchTask.getEvaluatorType());
    }

    @Test
    void switchExpressionContainsRoutingLogic() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        String expression = switchTask.getExpression();
        assertNotNull(expression);
        assertTrue(expression.contains("vip_high"), "Expression should route to vip_high");
        assertTrue(expression.contains("vip_standard"), "Expression should route to vip_standard");
        assertTrue(expression.contains("needs_review"), "Expression should route to needs_review");
        assertTrue(expression.contains("eu_processing"), "Expression should route to eu_processing");
        assertTrue(expression.contains("standard"), "Expression should route to standard");
        assertTrue(expression.contains("customerType"), "Expression should check customerType");
        assertTrue(expression.contains("amount"), "Expression should check amount");
        assertTrue(expression.contains("region"), "Expression should check region");
    }

    @Test
    void switchInputParametersReferenceWorkflowInput() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        Map<String, Object> inputParams = switchTask.getInputParameters();
        assertEquals("${workflow.input.amount}", inputParams.get("amount"));
        assertEquals("${workflow.input.customerType}", inputParams.get("customerType"));
        assertEquals("${workflow.input.region}", inputParams.get("region"));
    }

    @Test
    void switchHasFourDecisionCases() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        Map<String, List<WorkflowTask>> decisionCases = switchTask.getDecisionCases();
        assertEquals(4, decisionCases.size(), "SWITCH should have 4 decision cases");
        assertTrue(decisionCases.containsKey("vip_high"));
        assertTrue(decisionCases.containsKey("vip_standard"));
        assertTrue(decisionCases.containsKey("needs_review"));
        assertTrue(decisionCases.containsKey("eu_processing"));
    }

    @Test
    void vipHighCaseUsesVipConciergeTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> vipHighTasks = switchTask.getDecisionCases().get("vip_high");
        assertEquals(1, vipHighTasks.size());
        assertEquals("swjs_vip_concierge", vipHighTasks.get(0).getName());
        assertEquals("SIMPLE", vipHighTasks.get(0).getType());
    }

    @Test
    void vipStandardCaseUsesVipStandardTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("vip_standard");
        assertEquals(1, tasks.size());
        assertEquals("swjs_vip_standard", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void needsReviewCaseUsesManualReviewTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("needs_review");
        assertEquals(1, tasks.size());
        assertEquals("swjs_manual_review", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void euProcessingCaseUsesEuHandlerTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("eu_processing");
        assertEquals(1, tasks.size());
        assertEquals("swjs_eu_handler", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void defaultCaseUsesStandardTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> defaultTasks = switchTask.getDefaultCase();
        assertEquals(1, defaultTasks.size());
        assertEquals("swjs_standard", defaultTasks.get(0).getName());
        assertEquals("SIMPLE", defaultTasks.get(0).getType());
    }

    @Test
    void secondTaskIsFinalizeSimpleTask() {
        WorkflowTask finalizeTask = workflowDef.getTasks().get(1);
        assertEquals("swjs_finalize", finalizeTask.getName());
        assertEquals("finalize_ref", finalizeTask.getTaskReferenceName());
        assertEquals("SIMPLE", finalizeTask.getType());
    }

    @Test
    void workflowOutputParametersAreDefined() {
        Map<String, Object> outputParams = workflowDef.getOutputParameters();
        assertNotNull(outputParams);
        assertTrue(outputParams.containsKey("route"), "Output should include route");
        assertTrue(outputParams.containsKey("amount"), "Output should include amount");
    }

    @Test
    void workflowInputParametersIncludeExpectedFields() {
        List<String> inputParams = workflowDef.getInputParameters();
        assertTrue(inputParams.contains("amount"), "Workflow should accept 'amount' input");
        assertTrue(inputParams.contains("customerType"), "Workflow should accept 'customerType' input");
        assertTrue(inputParams.contains("region"), "Workflow should accept 'region' input");
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
    void taskDefsJsonHasSixEntries() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("task-defs.json")) {
            assertNotNull(is, "task-defs.json must exist on classpath");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<?> defs = mapper.readValue(json, List.class);
            assertEquals(6, defs.size(), "task-defs.json should have 6 task definitions");
        }
    }

    @Test
    void taskDefsJsonContainsAllTaskNames() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("task-defs.json")) {
            assertNotNull(is, "task-defs.json must exist on classpath");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<TaskDef> defs = mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, TaskDef.class));
            List<String> names = defs.stream().map(TaskDef::getName).toList();
            assertTrue(names.contains("swjs_vip_concierge"));
            assertTrue(names.contains("swjs_vip_standard"));
            assertTrue(names.contains("swjs_manual_review"));
            assertTrue(names.contains("swjs_eu_handler"));
            assertTrue(names.contains("swjs_standard"));
            assertTrue(names.contains("swjs_finalize"));
        }
    }
}
