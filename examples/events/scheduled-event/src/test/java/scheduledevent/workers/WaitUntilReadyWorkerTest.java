package scheduledevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitUntilReadyWorkerTest {

    private final WaitUntilReadyWorker worker = new WaitUntilReadyWorker();

    @Test
    void taskDefName() {
        assertEquals("se_wait_until_ready", worker.getTaskDefName());
    }

    @Test
    void waitsSuccessfully() {
        Task task = taskWith(Map.of("delayMs", 0, "eventId", "evt-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    @Test
    void outputContainsWaitedFlag() {
        Task task = taskWith(Map.of("delayMs", 5000, "eventId", "evt-002"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("waited"));
    }

    @Test
    void handlesZeroDelay() {
        Task task = taskWith(Map.of("delayMs", 0, "eventId", "evt-003"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    @Test
    void handlesLargeDelay() {
        Task task = taskWith(Map.of("delayMs", 60000, "eventId", "evt-004"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("delayMs", 0);
        input.put("eventId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    @Test
    void handlesNullDelayMs() {
        Map<String, Object> input = new HashMap<>();
        input.put("delayMs", null);
        input.put("eventId", "evt-005");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("waited"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
