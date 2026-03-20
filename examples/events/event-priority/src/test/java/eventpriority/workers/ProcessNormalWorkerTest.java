package eventpriority.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessNormalWorkerTest {

    private final ProcessNormalWorker worker = new ProcessNormalWorker();

    @Test
    void taskDefName() {
        assertEquals("pr_process_normal", worker.getTaskDefName());
    }

    @Test
    void processesEventWithPayload() {
        Task task = taskWith(Map.of("eventId", "evt-001", "payload", "order.created"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("normal", result.getOutputData().get("lane"));
    }

    @Test
    void outputAlwaysHasProcessedTrue() {
        Task task = taskWith(Map.of("eventId", "evt-002", "payload", "some-data"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputAlwaysHasLaneNormal() {
        Task task = taskWith(Map.of("eventId", "evt-003", "payload", "other-data"));
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("lane"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", "order.created");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("normal", result.getOutputData().get("lane"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("payload", null);
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
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("normal", result.getOutputData().get("lane"));
    }

    @Test
    void processesMultipleEventsConsistently() {
        Task task1 = taskWith(Map.of("eventId", "evt-a", "payload", "data-a"));
        Task task2 = taskWith(Map.of("eventId", "evt-b", "payload", "data-b"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals("normal", result1.getOutputData().get("lane"));
        assertEquals("normal", result2.getOutputData().get("lane"));
        assertEquals(true, result1.getOutputData().get("processed"));
        assertEquals(true, result2.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
