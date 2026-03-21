package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSubBWorkerTest {

    private final ProcessSubBWorker worker = new ProcessSubBWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_process_sub_b", worker.getTaskDefName());
    }

    @Test
    void processesCustomerInfoSubEvent() {
        Map<String, Object> subEvent = Map.of("type", "customer_info", "data", Map.of("id", "CUST-88", "name", "Alice"));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("customer_verified", result.getOutputData().get("result"));
        assertEquals("customer_info", result.getOutputData().get("subType"));
    }

    @Test
    void alwaysReturnsCustomerVerified() {
        Map<String, Object> subEvent = Map.of("type", "customer_info", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("customer_verified", result.getOutputData().get("result"));
    }

    @Test
    void alwaysReturnsCustomerInfoSubType() {
        Map<String, Object> subEvent = Map.of("type", "customer_info", "data", Map.of("name", "Bob"));
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals("customer_info", result.getOutputData().get("subType"));
    }

    @Test
    void handlesNullSubEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("subEvent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("customer_verified", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("customer_verified", result.getOutputData().get("result"));
        assertEquals("customer_info", result.getOutputData().get("subType"));
    }

    @Test
    void statusIsCompleted() {
        Map<String, Object> subEvent = Map.of("type", "customer_info", "data", Map.of());
        Task task = taskWith(Map.of("subEvent", subEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothResultAndSubType() {
        Map<String, Object> subEvent = Map.of("type", "customer_info", "data", Map.of("id", "CUST-99"));
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
