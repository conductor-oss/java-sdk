package securityincident.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemediateWorkerTest {

    private final RemediateWorker worker = new RemediateWorker();

    @Test
    void taskDefName() {
        assertEquals("si_remediate", worker.getTaskDefName());
    }

    @Test
    void remediatesWithInvestigateData() {
        Task task = taskWith(Map.of(
                "remediateData", Map.of("investigate", true, "processed", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediate"));
        assertEquals("2026-01-15T10:05:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void remediatesWithMinimalInput() {
        Task task = taskWith(Map.of("remediateData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediate"));
    }

    @Test
    void handlesNullRemediateData() {
        Map<String, Object> input = new HashMap<>();
        input.put("remediateData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediate"));
    }

    @Test
    void outputContainsCompletedAt() {
        Task task = taskWith(Map.of("remediateData", "data"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("completedAt"));
        assertEquals("2026-01-15T10:05:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("remediateData", Map.of("key", "val")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("remediate"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
