package eventschemavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterWorkerTest {

    private final DeadLetterWorker worker = new DeadLetterWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_dead_letter", worker.getTaskDefName());
    }

    @Test
    void sendsEventToDLQWithErrors() {
        List<String> errors = List.of("Missing required field: type", "Missing required field: source");
        Task task = taskWith(Map.of(
                "event", Map.of("data", Map.of("orderId", "ORD-100")),
                "errors", errors));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertEquals(2, outputErrors.size());
        assertEquals("Missing required field: type", outputErrors.get(0));
        assertEquals("Missing required field: source", outputErrors.get(1));
    }

    @Test
    void passesErrorsThrough() {
        List<String> errors = List.of("Missing required field: data");
        Task task = taskWith(Map.of(
                "event", Map.of("type", "t", "source", "s"),
                "errors", errors));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertEquals(errors, outputErrors);
    }

    @Test
    void handlesNullErrors() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", Map.of("type", "t"));
        input.put("errors", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertTrue(outputErrors.isEmpty());
    }

    @Test
    void handlesEmptyErrors() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "t", "source", "s", "data", "d"),
                "errors", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertTrue(outputErrors.isEmpty());
    }

    @Test
    void handlesSingleError() {
        Task task = taskWith(Map.of(
                "event", Map.of("source", "s", "data", "d"),
                "errors", List.of("Missing required field: type")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, outputErrors.size());
        assertEquals("Missing required field: type", outputErrors.get(0));
    }

    @Test
    void handlesThreeErrors() {
        List<String> errors = List.of(
                "Missing required field: type",
                "Missing required field: source",
                "Missing required field: data");
        Task task = taskWith(Map.of(
                "event", Map.of(),
                "errors", errors));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertEquals(3, outputErrors.size());
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        input.put("errors", List.of("Missing required field: type"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sentToDLQ"));
    }

    @Test
    void completesWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sentToDLQ"));
        @SuppressWarnings("unchecked")
        List<String> outputErrors = (List<String>) result.getOutputData().get("errors");
        assertTrue(outputErrors.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
