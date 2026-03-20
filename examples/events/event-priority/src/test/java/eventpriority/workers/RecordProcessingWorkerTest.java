package eventpriority.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordProcessingWorkerTest {

    private final RecordProcessingWorker worker = new RecordProcessingWorker();

    @Test
    void taskDefName() {
        assertEquals("pr_record_processing", worker.getTaskDefName());
    }

    @Test
    void recordsHighPriorityEvent() {
        Task task = taskWith(Map.of("eventId", "evt-001", "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void recordsMediumPriorityEvent() {
        Task task = taskWith(Map.of("eventId", "evt-002", "priority", "medium"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void recordsLowPriorityEvent() {
        Task task = taskWith(Map.of("eventId", "evt-003", "priority", "low"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void outputAlwaysHasRecordedTrue() {
        Task task = taskWith(Map.of("eventId", "evt-004", "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("priority", "high");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void handlesNullPriority() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("priority", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void recordsMultipleEventsConsistently() {
        Task task1 = taskWith(Map.of("eventId", "evt-a", "priority", "high"));
        Task task2 = taskWith(Map.of("eventId", "evt-b", "priority", "low"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(true, result1.getOutputData().get("recorded"));
        assertEquals(true, result2.getOutputData().get("recorded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
