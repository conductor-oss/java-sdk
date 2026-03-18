package scattergather.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SgrGather1WorkerTest {

    private final SgrGather1Worker worker = new SgrGather1Worker();

    @Test
    void taskDefName() {
        assertEquals("sgr_gather_1", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void responseContainsRequiredFields() {
        Task task = taskWith(Map.of("query", "laptop", "source", "price_service_a"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertNotNull(response);
        assertEquals("price_service_a", response.get("source"));
        assertNotNull(response.get("price"));
        assertEquals("USD", response.get("currency"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void priceIsDeterministic() {
        Task task1 = taskWith(Map.of("query", "laptop", "source", "price_service_a"));
        Task task2 = taskWith(Map.of("query", "laptop", "source", "price_service_a"));

        Map<String, Object> r1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("response");
        Map<String, Object> r2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("response");

        assertEquals(r1.get("price"), r2.get("price"), "Same inputs should produce same price");
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("response"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
