package customerjourney.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizeWorkerTest {

    private final OptimizeWorker worker = new OptimizeWorker();

    @Test
    void taskDefName() {
        assertEquals("cjy_optimize", worker.getTaskDefName());
    }

    @Test
    void returnsRecommendations() {
        Task task = taskWith(Map.of("insights", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("recommendations"));
        assertTrue(result.getOutputData().get("recommendations") instanceof List);
    }

    @SuppressWarnings("unchecked")
    @Test
    void recommendationsNotEmpty() {
        Task task = taskWith(Map.of("insights", Map.of()));
        TaskResult result = worker.execute(task);

        List<String> recs = (List<String>) result.getOutputData().get("recommendations");
        assertEquals(3, recs.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
