package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSubCWorkerTest {

    private final ProcessSubCWorker worker = new ProcessSubCWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_process_sub_c", worker.getTaskDefName());
    }

    @Test
    void processesShippingInfoSubEvent() {
        Map<String, Object> subEvent = Map.of("type", "shipping_info", "data", Map.of("method", "express", "address", "456 Oak Ave"));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipping_calculated", result.getOutputData().get("result"));
        assertEquals("shipping_info", result.getOutputData().get("subType"));
    }

    @Test
    void alwaysReturnsShippingCalculated() {
        Map<String, Object> subEvent = Map.of("type", "shipping_info", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("shipping_calculated", result.getOutputData().get("result"));
    }

    @Test
    void alwaysReturnsShippingInfoSubType() {
        Map<String, Object> subEvent = Map.of("type", "shipping_info", "data", Map.of("method", "overnight"));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("shipping_info", result.getOutputData().get("subType"));
    }

    @Test
    void handlesNullSubEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("subEvent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipping_calculated", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipping_calculated", result.getOutputData().get("result"));
        assertEquals("shipping_info", result.getOutputData().get("subType"));
    }

    @Test
    void statusIsCompleted() {
        Map<String, Object> subEvent = Map.of("type", "shipping_info", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothResultAndSubType() {
        Map<String, Object> subEvent = Map.of("type", "shipping_info", "data", Map.of("address", "789 Pine Rd"));
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
