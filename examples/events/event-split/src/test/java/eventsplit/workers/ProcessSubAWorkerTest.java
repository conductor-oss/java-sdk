package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSubAWorkerTest {

    private final ProcessSubAWorker worker = new ProcessSubAWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_process_sub_a", worker.getTaskDefName());
    }

    @Test
    void processesOrderDetailsSubEvent() {
        Map<String, Object> subEvent = Map.of("type", "order_details", "data", Map.of("orderId", "ORD-700"));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order_validated", result.getOutputData().get("result"));
        assertEquals("order_details", result.getOutputData().get("subType"));
    }

    @Test
    void alwaysReturnsOrderValidated() {
        Map<String, Object> subEvent = Map.of("type", "order_details", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("order_validated", result.getOutputData().get("result"));
    }

    @Test
    void alwaysReturnsOrderDetailsSubType() {
        Map<String, Object> subEvent = Map.of("type", "order_details", "data", Map.of("items", 5));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("order_details", result.getOutputData().get("subType"));
    }

    @Test
    void handlesNullSubEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("subEvent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order_validated", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order_validated", result.getOutputData().get("result"));
        assertEquals("order_details", result.getOutputData().get("subType"));
    }

    @Test
    void statusIsCompleted() {
        Map<String, Object> subEvent = Map.of("type", "order_details", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothResultAndSubType() {
        Map<String, Object> subEvent = Map.of("type", "order_details", "data", Map.of("total", 299.99));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("subType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
