package authorizationrbac.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatePermissionsWorkerTest {

    private final EvaluatePermissionsWorker worker = new EvaluatePermissionsWorker();

    @Test
    void taskDefName() {
        assertEquals("rbac_evaluate_permissions", worker.getTaskDefName());
    }

    @Test
    void evaluatesPermissionsSuccessfully() {
        Task task = taskWith(Map.of("action", "delete", "resource", "billing/invoices"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("evaluate_permissions"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsPermissions() {
        Task task = taskWith(Map.of("action", "read", "resource", "users"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("evaluate_permissions"));
    }

    @Test
    void outputContainsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullAction() {
        Map<String, Object> input = new HashMap<>();
        input.put("action", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyAction() {
        Task task = taskWith(Map.of("action", "", "resource", ""));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of("action", "write", "resource", "docs"));
        Task t2 = taskWith(Map.of("action", "write", "resource", "docs"));
        assertEquals(worker.execute(t1).getOutputData(), worker.execute(t2).getOutputData());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
