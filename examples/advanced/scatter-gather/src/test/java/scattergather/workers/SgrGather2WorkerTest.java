package scattergather.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SgrGather2WorkerTest {

    private final SgrGather2Worker worker = new SgrGather2Worker();

    @Test
    void taskDefName() {
        assertEquals("sgr_gather_2", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void responseContainsRequiredFields() {
        Task task = taskWith(Map.of("query", "phone", "source", "price_service_b"));
        TaskResult result = worker.execute(task);

        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertNotNull(response);
        assertEquals("price_service_b", response.get("source"));
        assertTrue(response.get("price") instanceof Number);
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
