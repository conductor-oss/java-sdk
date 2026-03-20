package eventversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("vr_process_event", worker.getTaskDefName());
    }

    @Test
    void processesEventWithV1Version() {
        Task task = taskWith(Map.of("originalVersion", "v1", "event", Map.of("name", "John")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("v1", result.getOutputData().get("originalVersion"));
    }

    @Test
    void processesEventWithV2Version() {
        Task task = taskWith(Map.of("originalVersion", "v2", "event", Map.of("data", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("v2", result.getOutputData().get("originalVersion"));
    }

    @Test
    void processesEventWithV3Version() {
        Task task = taskWith(Map.of("originalVersion", "v3", "event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("v3", result.getOutputData().get("originalVersion"));
    }

    @Test
    void alwaysOutputsProcessedTrue() {
        Task task = taskWith(Map.of("originalVersion", "v1", "event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullOriginalVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("originalVersion", null);
        input.put("event", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("unknown", result.getOutputData().get("originalVersion"));
    }

    @Test
    void handlesMissingOriginalVersion() {
        Task task = taskWith(Map.of("event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("originalVersion"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("unknown", result.getOutputData().get("originalVersion"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
