package taskinputtemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteActionWorkerTest {

    private final ExecuteActionWorker worker = new ExecuteActionWorker();

    @Test
    void taskDefName() {
        assertEquals("tpl_execute_action", worker.getTaskDefName());
    }

    @Test
    void adminWriteIsAllowed() {
        Task task = taskWith(Map.of(
                "userName", "Alice",
                "userRole", "admin",
                "action", "write",
                "context", Map.of("permissions", List.of("read", "write", "delete"))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> auditLog = (Map<String, Object>) result.getOutputData().get("auditLog");
        assertEquals("Alice", auditLog.get("user"));
        assertEquals("write", auditLog.get("action"));
        assertEquals(true, auditLog.get("allowed"));
        assertNotNull(auditLog.get("timestamp"));
    }

    @Test
    void viewerWriteIsDenied() {
        Task task = taskWith(Map.of(
                "userName", "Bob",
                "userRole", "viewer",
                "action", "write",
                "context", Map.of("permissions", List.of("read"))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("permission_denied", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> auditLog = (Map<String, Object>) result.getOutputData().get("auditLog");
        assertEquals(false, auditLog.get("allowed"));
    }

    @Test
    void viewerReadIsAllowed() {
        Task task = taskWith(Map.of(
                "userName", "Bob",
                "userRole", "viewer",
                "action", "read",
                "context", Map.of("permissions", List.of("read"))
        ));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> auditLog = (Map<String, Object>) result.getOutputData().get("auditLog");
        assertEquals(true, auditLog.get("allowed"));
    }

    @Test
    void auditLogContainsAllFields() {
        Task task = taskWith(Map.of(
                "userName", "Alice",
                "userRole", "admin",
                "action", "delete",
                "context", Map.of("permissions", List.of("read", "write", "delete"))
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> auditLog = (Map<String, Object>) result.getOutputData().get("auditLog");
        assertTrue(auditLog.containsKey("user"));
        assertTrue(auditLog.containsKey("action"));
        assertTrue(auditLog.containsKey("allowed"));
        assertTrue(auditLog.containsKey("timestamp"));
        assertEquals("delete", auditLog.get("action"));
    }

    @Test
    void guestDeleteIsDenied() {
        Task task = taskWith(Map.of(
                "userName", "Unknown",
                "userRole", "guest",
                "action", "delete",
                "context", Map.of("permissions", List.of("read"))
        ));
        TaskResult result = worker.execute(task);

        assertEquals("permission_denied", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
