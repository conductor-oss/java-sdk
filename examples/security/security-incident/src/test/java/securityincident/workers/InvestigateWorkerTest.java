package securityincident.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InvestigateWorkerTest {

    private final InvestigateWorker worker = new InvestigateWorker();

    @Test
    void taskDefName() {
        assertEquals("si_investigate", worker.getTaskDefName());
    }

    @Test
    void investigatesWithContainData() {
        Task task = taskWith(Map.of(
                "investigateData", Map.of("contain", true, "processed", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("investigate"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void investigatesWithMinimalInput() {
        Task task = taskWith(Map.of("investigateData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("investigate"));
    }

    @Test
    void handlesNullInvestigateData() {
        Map<String, Object> input = new HashMap<>();
        input.put("investigateData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("investigate"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("investigateData", "data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("investigate"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void investigateValueIsTrue() {
        Task task = taskWith(Map.of("investigateData", Map.of("key", "val")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("investigate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
