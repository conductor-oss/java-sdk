package changemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImplementChangeTest {

    private final ImplementChange worker = new ImplementChange();

    @Test
    void taskDefName() {
        assertEquals("cm_implement", worker.getTaskDefName());
    }

    @Test
    void implementsApprovedChange() {
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("implemented"));
        assertNotNull(result.getOutputData().get("completedAt"));
        assertNotNull(result.getOutputData().get("rollbackPlan"));
    }

    @Test
    void implementsEvenWithoutApproval() {
        Task task = taskWith(Map.of("approved", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("implemented"));
    }

    @Test
    void completedAtIsDeterministic() {
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-14T10:00:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void rollbackPlanPresent() {
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertEquals("Restore previous RDS snapshot", result.getOutputData().get("rollbackPlan"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("implementData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("implemented"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("implemented"));
        assertNotNull(result.getOutputData().get("completedAt"));
        assertNotNull(result.getOutputData().get("rollbackPlan"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("implementData", dataMap);
        task.setInputData(input);
        return task;
    }
}
