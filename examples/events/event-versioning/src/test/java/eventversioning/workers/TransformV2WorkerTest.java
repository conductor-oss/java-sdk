package eventversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformV2WorkerTest {

    private final TransformV2Worker worker = new TransformV2Worker();

    @Test
    void taskDefName() {
        assertEquals("vr_transform_v2", worker.getTaskDefName());
    }

    @Test
    void transformsV2EventToV3() {
        Task task = taskWith(Map.of("event", Map.of("version", "v2", "name", "Alice")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertNotNull(transformed);
        assertEquals("v3", transformed.get("version"));
        assertEquals("v2", transformed.get("migratedFrom"));
        assertEquals("Alice", transformed.get("name"));
    }

    @Test
    void preservesOriginalEventFields() {
        Task task = taskWith(Map.of("event", Map.of("version", "v2", "type", "order.placed", "total", 99.99)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("order.placed", transformed.get("type"));
        assertEquals(99.99, transformed.get("total"));
    }

    @Test
    void overridesVersionToV3() {
        Task task = taskWith(Map.of("event", Map.of("version", "v2")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
    }

    @Test
    void setsMigratedFromV2() {
        Task task = taskWith(Map.of("event", Map.of("version", "v2")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v2", transformed.get("migratedFrom"));
    }

    @Test
    void handlesEmptyEvent() {
        Task task = taskWith(Map.of("event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
        assertEquals("v2", transformed.get("migratedFrom"));
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
        assertEquals("v2", transformed.get("migratedFrom"));
    }

    @Test
    void handlesMissingEventKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertNotNull(transformed);
        assertEquals("v3", transformed.get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
