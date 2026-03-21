package eventdrivenworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandleGenericWorkerTest {

    private final HandleGenericWorker worker = new HandleGenericWorker();

    @Test
    void taskDefName() {
        assertEquals("ed_handle_generic", worker.getTaskDefName());
    }

    @Test
    void handlesGenericEvent() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("key", "value"),
                "category", "generic"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("generic", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("generic", result.getOutputData().get("category"));
    }

    @Test
    void preservesCategoryFromInput() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("sku", "ITEM-42"),
                "category", "inventory"));
        TaskResult result = worker.execute(task);

        assertEquals("inventory", result.getOutputData().get("category"));
    }

    @Test
    void outputAlwaysHasHandlerGeneric() {
        Task task = taskWith(Map.of(
                "eventData", Map.of(),
                "category", "generic"));
        TaskResult result = worker.execute(task);

        assertEquals("generic", result.getOutputData().get("handler"));
    }

    @Test
    void outputAlwaysHasProcessedTrue() {
        Task task = taskWith(Map.of(
                "eventData", Map.of(),
                "category", "generic"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullCategory() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventData", Map.of());
        input.put("category", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("generic", result.getOutputData().get("category"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("generic", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNotificationCategory() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("message", "alert"),
                "category", "notification"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification", result.getOutputData().get("category"));
        assertEquals("generic", result.getOutputData().get("handler"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
