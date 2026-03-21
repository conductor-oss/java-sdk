package serverlessorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SvlAggregateWorkerTest {

    private final SvlAggregateWorker worker = new SvlAggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("svl_aggregate", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "EVT-001",
                "score", 87.5,
                "enriched", Map.of("userTier", "premium")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAggregatedTrue() {
        Task task = taskWith(Map.of("eventId", "EVT-002", "score", 90.0));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("aggregated"));
    }

    @Test
    void outputContainsStoredTrue() {
        Task task = taskWith(Map.of("eventId", "EVT-003", "score", 75.0));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("score", 50.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("aggregated"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputHasTwoFields() {
        Task task = taskWith(Map.of("eventId", "EVT-005", "score", 80.0));
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().size());
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of("eventId", "EVT-A", "score", 87.5));
        Task t2 = taskWith(Map.of("eventId", "EVT-B", "score", 87.5));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("aggregated"), r2.getOutputData().get("aggregated"));
        assertEquals(r1.getOutputData().get("stored"), r2.getOutputData().get("stored"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
