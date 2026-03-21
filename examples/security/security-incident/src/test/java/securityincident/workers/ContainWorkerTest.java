package securityincident.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContainWorkerTest {

    private final ContainWorker worker = new ContainWorker();

    @Test
    void taskDefName() {
        assertEquals("si_contain", worker.getTaskDefName());
    }

    @Test
    void containsWithTriageData() {
        Task task = taskWith(Map.of(
                "containData", Map.of("triageId", "TRIAGE-1381", "success", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("contain"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void containsWithMinimalInput() {
        Task task = taskWith(Map.of("containData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("contain"));
    }

    @Test
    void handlesNullContainData() {
        Map<String, Object> input = new HashMap<>();
        input.put("containData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("contain"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("containData", Map.of("triageId", "T-1")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("contain"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("containData", "some-data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void containValueIsTrue() {
        Task task = taskWith(Map.of("containData", Map.of("key", "value")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("contain"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
