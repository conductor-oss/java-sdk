package eventversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectVersionWorkerTest {

    private final DetectVersionWorker worker = new DetectVersionWorker();

    @Test
    void taskDefName() {
        assertEquals("vr_detect_version", worker.getTaskDefName());
    }

    @Test
    void detectsVersionFieldV1() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1", "type", "user.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("v1", result.getOutputData().get("version"));
    }

    @Test
    void detectsVersionFieldV2() {
        Task task = taskWith(Map.of("event", Map.of("version", "v2", "data", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("v2", result.getOutputData().get("version"));
    }

    @Test
    void detectsSchemaVersionWhenVersionMissing() {
        Task task = taskWith(Map.of("event", Map.of("schemaVersion", "v2", "type", "order")));
        TaskResult result = worker.execute(task);

        assertEquals("v2", result.getOutputData().get("version"));
    }

    @Test
    void prefersVersionOverSchemaVersion() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1", "schemaVersion", "v2")));
        TaskResult result = worker.execute(task);

        assertEquals("v1", result.getOutputData().get("version"));
    }

    @Test
    void defaultsToV3WhenNoVersionFields() {
        Task task = taskWith(Map.of("event", Map.of("type", "user.created", "name", "John")));
        TaskResult result = worker.execute(task);

        assertEquals("v3", result.getOutputData().get("version"));
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("v3", result.getOutputData().get("version"));
    }

    @Test
    void handlesMissingEventKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("v3", result.getOutputData().get("version"));
    }

    @Test
    void handlesEmptyEvent() {
        Task task = taskWith(Map.of("event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("v3", result.getOutputData().get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
