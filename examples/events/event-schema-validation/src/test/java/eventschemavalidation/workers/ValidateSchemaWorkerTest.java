package eventschemavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSchemaWorkerTest {

    private final ValidateSchemaWorker worker = new ValidateSchemaWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_validate_schema", worker.getTaskDefName());
    }

    @Test
    void validEventWithAllRequiredFields() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "order.created", "source", "shop-api",
                        "data", Map.of("orderId", "ORD-100")),
                "schemaName", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.isEmpty());
        assertEquals("order_event_v1", result.getOutputData().get("schemaUsed"));
    }

    @Test
    void invalidEventMissingType() {
        Map<String, Object> event = new HashMap<>();
        event.put("source", "shop-api");
        event.put("data", Map.of("orderId", "ORD-100"));
        Task task = taskWith(Map.of("event", event, "schemaName", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
        assertEquals("Missing required field: type", errors.get(0));
    }

    @Test
    void invalidEventMissingSource() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "order.created");
        event.put("data", Map.of("orderId", "ORD-100"));
        Task task = taskWith(Map.of("event", event, "schemaName", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
        assertEquals("Missing required field: source", errors.get(0));
    }

    @Test
    void invalidEventMissingData() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "order.created");
        event.put("source", "shop-api");
        Task task = taskWith(Map.of("event", event, "schemaName", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
        assertEquals("Missing required field: data", errors.get(0));
    }

    @Test
    void invalidEventMissingMultipleFields() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "order.created");
        Task task = taskWith(Map.of("event", event, "schemaName", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(2, errors.size());
        assertTrue(errors.contains("Missing required field: source"));
        assertTrue(errors.contains("Missing required field: data"));
    }

    @Test
    void nullEventProducesThreeErrors() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        input.put("schemaName", "order_event_v1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(3, errors.size());
    }

    @Test
    void emptyEventProducesThreeErrors() {
        Task task = taskWith(Map.of("event", Map.of(), "schemaName", "test_schema"));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(3, errors.size());
        assertTrue(errors.contains("Missing required field: type"));
        assertTrue(errors.contains("Missing required field: source"));
        assertTrue(errors.contains("Missing required field: data"));
    }

    @Test
    void defaultSchemaNameWhenMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", Map.of("type", "t", "source", "s", "data", "d"));
        input.put("schemaName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("valid", result.getOutputData().get("result"));
        assertEquals("default_schema", result.getOutputData().get("schemaUsed"));
    }

    @Test
    void defaultSchemaNameWhenBlank() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "t", "source", "s", "data", "d"),
                "schemaName", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("default_schema", result.getOutputData().get("schemaUsed"));
    }

    @Test
    void schemaNamePassedThrough() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "t", "source", "s", "data", "d"),
                "schemaName", "custom_v2"));
        TaskResult result = worker.execute(task);

        assertEquals("custom_v2", result.getOutputData().get("schemaUsed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
