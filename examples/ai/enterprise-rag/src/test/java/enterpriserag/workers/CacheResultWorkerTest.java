package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheResultWorkerTest {

    private final CacheResultWorker worker = new CacheResultWorker();

    @Test
    void taskDefName() {
        assertEquals("er_cache_result", worker.getTaskDefName());
    }

    @Test
    void returnsCachedStatus() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "What is RAG?");
        input.put("answer", "RAG is retrieval-augmented generation.");
        input.put("ttlSeconds", 3600);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cached"));
        assertNotNull(result.getOutputData().get("cacheKey"));
        assertNotNull(result.getOutputData().get("expiresAt"));
    }

    @Test
    void cacheKeyIsDeterministic() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "What is RAG?");
        input.put("answer", "An answer");
        input.put("ttlSeconds", 3600);

        Task task1 = taskWith(input);
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(input);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("cacheKey"),
                result2.getOutputData().get("cacheKey"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
