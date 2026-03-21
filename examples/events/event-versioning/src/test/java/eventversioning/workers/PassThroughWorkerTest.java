package eventversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PassThroughWorkerTest {

    private final PassThroughWorker worker = new PassThroughWorker();

    @Test
    void taskDefName() {
        assertEquals("vr_pass_through", worker.getTaskDefName());
    }

    @Test
    void passesEventThrough() {
        Map<String, Object> event = Map.of("version", "v3", "type", "user.created", "name", "Bob");
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(event, result.getOutputData().get("transformed"));
    }

    @Test
    void outputMatchesInput() {
        Map<String, Object> event = Map.of("version", "v3", "data", "test");
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformed");
        assertEquals("v3", transformed.get("version"));
        assertEquals("test", transformed.get("data"));
    }

    @Test
    void handlesEventWithManyFields() {
        Map<String, Object> event = Map.of(
                "version", "v3", "type", "order", "id", "123", "amount", 50.0);
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        assertEquals(event, result.getOutputData().get("transformed"));
    }

    @Test
    void handlesEmptyEvent() {
        Task task = taskWith(Map.of("event", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(Map.of(), result.getOutputData().get("transformed"));
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(Map.of(), result.getOutputData().get("transformed"));
    }

    @Test
    void handlesMissingEventKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(Map.of(), result.getOutputData().get("transformed"));
    }

    @Test
    void doesNotModifyOriginalEvent() {
        Map<String, Object> event = Map.of("version", "v3", "name", "Test");
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        Object transformed = result.getOutputData().get("transformed");
        assertSame(event, transformed);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
