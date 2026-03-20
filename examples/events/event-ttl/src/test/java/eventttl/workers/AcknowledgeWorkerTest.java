package eventttl.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AcknowledgeWorkerTest {

    private final AcknowledgeWorker worker = new AcknowledgeWorker();

    @Test
    void taskDefName() {
        assertEquals("xl_acknowledge", worker.getTaskDefName());
    }

    @Test
    void acknowledgesEventSuccessfully() {
        Task task = taskWith(Map.of("eventId", "evt-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void outputContainsAcknowledgedTrue() {
        Task task = taskWith(Map.of("eventId", "evt-002"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesMissingEventId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void completesForDifferentEventIds() {
        Task task1 = taskWith(Map.of("eventId", "evt-aaa"));
        Task task2 = taskWith(Map.of("eventId", "evt-bbb"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result1.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("eventId", "evt-006"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void acknowledgedValueIsBoolean() {
        Task task = taskWith(Map.of("eventId", "evt-007"));
        TaskResult result = worker.execute(task);

        Object acknowledged = result.getOutputData().get("acknowledged");
        assertInstanceOf(Boolean.class, acknowledged);
        assertEquals(true, acknowledged);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
