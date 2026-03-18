package scattergather.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SgrGather3WorkerTest {

    private final SgrGather3Worker worker = new SgrGather3Worker();

    @Test
    void taskDefName() {
        assertEquals("sgr_gather_3", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void responseContainsRequiredFields() {
        Task task = taskWith(Map.of("query", "tablet", "source", "price_service_c"));
        TaskResult result = worker.execute(task);

        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertNotNull(response);
        assertEquals("price_service_c", response.get("source"));
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
