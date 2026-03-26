package logprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractPatternsWorkerTest {

    private final ExtractPatternsWorker worker = new ExtractPatternsWorker();

    @Test
    void taskDefName() {
        assertEquals("lp_extract_patterns", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsPatterns() {
        List<Map<String, Object>> entries = List.of(
                Map.of("message", "Token validation failed: expired", "isError", true),
                Map.of("message", "Token validation failed: expired", "isError", true),
                Map.of("message", "Request received GET /api/users", "isError", false));
        Task task = taskWith(Map.of("entries", entries));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) result.getOutputData().get("patterns");
        assertNotNull(patterns);
        assertTrue((int) result.getOutputData().get("patternCount") > 0);
    }

    @Test
    void topPatternIsCorrect() {
        List<Map<String, Object>> entries = List.of(
                Map.of("message", "same message", "isError", false),
                Map.of("message", "same message", "isError", false),
                Map.of("message", "different message", "isError", false));
        Task task = taskWith(Map.of("entries", entries));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("topPattern"));
    }

    @Test
    void handlesEmptyEntries() {
        Task task = taskWith(Map.of("entries", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("patternCount"));
        assertEquals("none", result.getOutputData().get("topPattern"));
    }

    @Test
    void handlesNullEntries() {
        Map<String, Object> input = new HashMap<>();
        input.put("entries", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("patternCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
