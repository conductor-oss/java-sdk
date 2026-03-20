package authorizationrbac.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveRolesWorkerTest {

    private final ResolveRolesWorker worker = new ResolveRolesWorker();

    @Test
    void taskDefName() {
        assertEquals("rbac_resolve_roles", worker.getTaskDefName());
    }

    @Test
    void resolvesRolesSuccessfully() {
        Task task = taskWith(Map.of("userId", "user-001", "resource", "billing/invoices", "action", "delete"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RESOLVE_ROLES-1372", result.getOutputData().get("resolve_rolesId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsId() {
        Task task = taskWith(Map.of("userId", "user-002"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("resolve_rolesId"));
    }

    @Test
    void outputContainsSuccess() {
        Task task = taskWith(Map.of("userId", "user-003"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
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
    void handlesEmptyUserId() {
        Task task = taskWith(Map.of("userId", ""));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of("userId", "u1"));
        Task t2 = taskWith(Map.of("userId", "u1"));
        assertEquals(worker.execute(t1).getOutputData().get("resolve_rolesId"),
                     worker.execute(t2).getOutputData().get("resolve_rolesId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
