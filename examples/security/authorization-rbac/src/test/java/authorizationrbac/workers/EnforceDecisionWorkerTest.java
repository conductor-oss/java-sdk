package authorizationrbac.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnforceDecisionWorkerTest {

    private final EnforceDecisionWorker worker = new EnforceDecisionWorker();

    @Test
    void taskDefName() {
        assertEquals("rbac_enforce_decision", worker.getTaskDefName());
    }

    @Test
    void enforcesDecisionSuccessfully() {
        Task task = taskWith(Map.of("enforce_decisionData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enforce_decision"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void outputContainsEnforceDecision() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("enforce_decision"));
    }

    @Test
    void outputContainsCompletedAt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesExtraInputs() {
        Task task = taskWith(Map.of("extra", "value"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of());
        Task t2 = taskWith(Map.of());
        assertEquals(worker.execute(t1).getOutputData().get("enforce_decision"),
                     worker.execute(t2).getOutputData().get("enforce_decision"));
    }

    @Test
    void completedAtFormat() {
        Task task = taskWith(Map.of());
        String completedAt = (String) worker.execute(task).getOutputData().get("completedAt");
        assertTrue(completedAt.contains("T"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
