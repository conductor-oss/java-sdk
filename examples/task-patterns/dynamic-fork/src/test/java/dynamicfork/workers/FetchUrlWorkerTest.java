package dynamicfork.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchUrlWorkerTest {

    private final FetchUrlWorker worker = new FetchUrlWorker();

    @Test
    void taskDefName() {
        assertEquals("df_fetch_url", worker.getTaskDefName());
    }

    @Test
    void fetchesUrlWithDeterministicOutput() {
        Task task = taskWith(Map.of("url", "https://example.com", "index", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://example.com", result.getOutputData().get("url"));
        assertEquals(200, result.getOutputData().get("status"));

        // Deterministic: size = 1024 + (19 * 100) + (0 * 256) = 2924
        assertEquals(2924, result.getOutputData().get("size"));
        // Deterministic: loadTime = 50 + (19 * 5) + (0 * 10) = 145
        assertEquals(145, result.getOutputData().get("loadTime"));
    }

    @Test
    void fetchUrlAtDifferentIndex() {
        Task task = taskWith(Map.of("url", "https://example.com", "index", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // size = 1024 + (19 * 100) + (2 * 256) = 3436
        assertEquals(3436, result.getOutputData().get("size"));
        // loadTime = 50 + (19 * 5) + (2 * 10) = 165
        assertEquals(165, result.getOutputData().get("loadTime"));
    }

    @Test
    void differentUrlProducesDifferentSize() {
        Task task1 = taskWith(Map.of("url", "https://a.com", "index", 0));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("url", "https://longer-url.example.com", "index", 0));
        TaskResult result2 = worker.execute(task2);

        int size1 = (int) result1.getOutputData().get("size");
        int size2 = (int) result2.getOutputData().get("size");
        assertNotEquals(size1, size2);
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("url", "https://test.com", "index", 1));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("url", "https://test.com", "index", 1));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("size"), result2.getOutputData().get("size"));
        assertEquals(result1.getOutputData().get("loadTime"), result2.getOutputData().get("loadTime"));
    }

    @Test
    void handlesNullUrl() {
        Task task = taskWith(Map.of("index", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("url"));
    }

    @Test
    void handlesBlankUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("url", "   ");
        input.put("index", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("url"));
    }

    @Test
    void handlesMissingIndex() {
        Task task = taskWith(Map.of("url", "https://test.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // index defaults to 0, so size = 1024 + (16 * 100) + (0 * 256) = 2624
        assertEquals(2624, result.getOutputData().get("size"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
