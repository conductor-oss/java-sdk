package dataexport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareDataWorkerTest {

    private final PrepareDataWorker worker = new PrepareDataWorker();

    @Test
    void taskDefName() {
        assertEquals("dx_prepare_data", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsData() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void returnsHeaders() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) result.getOutputData().get("headers");
        assertNotNull(headers);
        assertTrue(headers.contains("id"));
        assertTrue(headers.contains("name"));
    }

    @Test
    void recordCountMatchesData() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(5, result.getOutputData().get("recordCount"));
    }

    @Test
    void returnsConsistentResults() {
        Task task = taskWith(Map.of());
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("recordCount"), r2.getOutputData().get("recordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
