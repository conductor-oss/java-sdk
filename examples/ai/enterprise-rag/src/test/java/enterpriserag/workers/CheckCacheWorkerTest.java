package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCacheWorkerTest {

    private final CheckCacheWorker worker = new CheckCacheWorker();

    @Test
    void taskDefName() {
        assertEquals("er_check_cache", worker.getTaskDefName());
    }

    @Test
    void returnsCacheMiss() {
        Task task = taskWith(Map.of(
                "question", "What is RAG?",
                "userId", "user-1"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
        assertNull(result.getOutputData().get("cachedAnswer"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
