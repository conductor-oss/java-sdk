package changemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubmitChangeTest {

    private final SubmitChange worker = new SubmitChange();

    @Test
    void taskDefName() {
        assertEquals("cm_submit", worker.getTaskDefName());
    }

    @Test
    void submitsStandardChange() {
        Task task = taskWith("standard", "Upgrade RDS to PostgreSQL 16", "orders-db");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUBMIT-1351", result.getOutputData().get("submitId"));
        assertEquals("standard", result.getOutputData().get("changeType"));
        assertEquals("Upgrade RDS to PostgreSQL 16", result.getOutputData().get("description"));
        assertEquals("orders-db", result.getOutputData().get("system"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void submitsEmergencyChange() {
        Task task = taskWith("emergency", "Fix critical security vulnerability", "auth-service");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("emergency", result.getOutputData().get("changeType"));
        assertEquals("auth-service", result.getOutputData().get("system"));
    }

    @Test
    void submitsNormalChange() {
        Task task = taskWith("normal", "Add monitoring dashboard", "metrics-service");
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("changeType"));
        assertEquals("Add monitoring dashboard", result.getOutputData().get("description"));
    }

    @Test
    void handlesNullChangeType() {
        Map<String, Object> input = new HashMap<>();
        input.put("changeType", null);
        input.put("description", "Some change");
        input.put("system", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("standard", result.getOutputData().get("changeType"));
    }

    @Test
    void handlesNullDescription() {
        Map<String, Object> input = new HashMap<>();
        input.put("changeType", "standard");
        input.put("description", null);
        input.put("system", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("No description provided", result.getOutputData().get("description"));
    }

    @Test
    void handlesNullSystem() {
        Map<String, Object> input = new HashMap<>();
        input.put("changeType", "standard");
        input.put("description", "Test");
        input.put("system", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("system"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("standard", result.getOutputData().get("changeType"));
        assertEquals("No description provided", result.getOutputData().get("description"));
        assertEquals("unknown", result.getOutputData().get("system"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsSubmitId() {
        Task task = taskWith("standard", "Test", "sys");
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("submitId"));
        assertTrue(result.getOutputData().get("submitId").toString().startsWith("SUBMIT-"));
    }

    private Task taskWith(String changeType, String description, String system) {
        Map<String, Object> input = new HashMap<>();
        input.put("changeType", changeType);
        input.put("description", description);
        input.put("system", system);
        return taskWith(input);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
