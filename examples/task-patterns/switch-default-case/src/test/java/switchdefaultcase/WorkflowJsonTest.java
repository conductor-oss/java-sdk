package switchdefaultcase;

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
 * with defaultCase for unmatched payment methods.
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
        assertEquals("default_case_demo", workflowDef.getName());
        assertEquals(1, workflowDef.getVersion());
    }

    @Test
    void schemaVersionIsTwo() {
        assertEquals(2, workflowDef.getSchemaVersion());
    }

    @Test
    void hasTwoTopLevelTasks() {
        List<WorkflowTask> tasks = workflowDef.getTasks();
        assertEquals(2, tasks.size(), "Workflow should have SWITCH + dc_log = 2 top-level tasks");
    }

    @Test
    void firstTaskIsSwitchType() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        assertEquals("SWITCH", switchTask.getType());
        assertEquals("route_by_payment", switchTask.getName());
        assertEquals("route_ref", switchTask.getTaskReferenceName());
    }

    @Test
    void switchUsesValueParamEvaluator() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        assertEquals("value-param", switchTask.getEvaluatorType());
        assertEquals("switchCaseValue", switchTask.getExpression());
    }

    @Test
    void switchInputParametersReferenceWorkflowInput() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        Map<String, Object> inputParams = switchTask.getInputParameters();
        assertEquals("${workflow.input.paymentMethod}", inputParams.get("switchCaseValue"));
    }

    @Test
    void switchHasThreeDecisionCases() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        Map<String, List<WorkflowTask>> decisionCases = switchTask.getDecisionCases();
        assertEquals(3, decisionCases.size(), "SWITCH should have 3 decision cases");
        assertTrue(decisionCases.containsKey("credit_card"));
        assertTrue(decisionCases.containsKey("bank_transfer"));
        assertTrue(decisionCases.containsKey("crypto"));
    }

    @Test
    void creditCardCaseUsesProcessCardTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("credit_card");
        assertEquals(1, tasks.size());
        assertEquals("dc_process_card", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void bankTransferCaseUsesProcessBankTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("bank_transfer");
        assertEquals(1, tasks.size());
        assertEquals("dc_process_bank", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void cryptoCaseUsesProcessCryptoTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> tasks = switchTask.getDecisionCases().get("crypto");
        assertEquals(1, tasks.size());
        assertEquals("dc_process_crypto", tasks.get(0).getName());
        assertEquals("SIMPLE", tasks.get(0).getType());
    }

    @Test
    void defaultCaseUsesUnknownMethodTask() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> defaultTasks = switchTask.getDefaultCase();
        assertEquals(1, defaultTasks.size());
        assertEquals("dc_unknown_method", defaultTasks.get(0).getName());
        assertEquals("SIMPLE", defaultTasks.get(0).getType());
    }

    @Test
    void defaultCaseInputUsesMethodParam() {
        WorkflowTask switchTask = workflowDef.getTasks().get(0);
        List<WorkflowTask> defaultTasks = switchTask.getDefaultCase();
        Map<String, Object> inputParams = defaultTasks.get(0).getInputParameters();
        assertEquals("${workflow.input.paymentMethod}", inputParams.get("method"));
    }

    @Test
    void secondTaskIsLogSimpleTask() {
        WorkflowTask logTask = workflowDef.getTasks().get(1);
        assertEquals("dc_log", logTask.getName());
        assertEquals("log_ref", logTask.getTaskReferenceName());
        assertEquals("SIMPLE", logTask.getType());
    }

    @Test
    void workflowOutputParametersAreDefined() {
        Map<String, Object> outputParams = workflowDef.getOutputParameters();
        assertNotNull(outputParams);
        assertTrue(outputParams.containsKey("method"), "Output should include method");
        assertTrue(outputParams.containsKey("logged"), "Output should include logged");
    }

    @Test
    void workflowInputParametersIncludeExpectedFields() {
        List<String> inputParams = workflowDef.getInputParameters();
        assertTrue(inputParams.contains("paymentMethod"), "Workflow should accept 'paymentMethod' input");
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
    void taskDefsJsonHasFiveEntries() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("task-defs.json")) {
            assertNotNull(is, "task-defs.json must exist on classpath");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<?> defs = mapper.readValue(json, List.class);
            assertEquals(5, defs.size(), "task-defs.json should have 5 task definitions");
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
            assertTrue(names.contains("dc_process_card"));
            assertTrue(names.contains("dc_process_bank"));
            assertTrue(names.contains("dc_process_crypto"));
            assertTrue(names.contains("dc_unknown_method"));
            assertTrue(names.contains("dc_log"));
        }
    }
}
