package eventversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformV1WorkerTest {

    private final TransformV1Worker worker = new TransformV1Worker();

    @Test
    void taskDefName() {
        assertEquals("vr_transform_v1", worker.getTaskDefName());
    }

    @Test
    void transformsV1EventToV3() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1", "name", "John")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertNotNull(transformed);
        assertEquals("v3", transformed.get("version"));
        assertEquals("v1", transformed.get("migratedFrom"));
        assertEquals("John", transformed.get("name"));
    }

    @Test
    void preservesOriginalEventFields() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1", "type", "user.created", "email", "a@b.com")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("user.created", transformed.get("type"));
        assertEquals("a@b.com", transformed.get("email"));
    }

    @Test
    void overridesVersionToV3() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
    }

    @Test
    void setsMigratedFromV1() {
        Task task = taskWith(Map.of("event", Map.of("version", "v1")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v1", transformed.get("migratedFrom"));
    }

    @Test
    void handlesEmptyEvent() {
        Task task = taskWith(Map.of("event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
        assertEquals("v1", transformed.get("migratedFrom"));
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
        assertEquals("v1", transformed.get("migratedFrom"));
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
