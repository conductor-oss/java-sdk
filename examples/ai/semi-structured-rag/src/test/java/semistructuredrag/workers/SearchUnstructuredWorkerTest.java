package semistructuredrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchUnstructuredWorkerTest {

    private final SearchUnstructuredWorker worker = new SearchUnstructuredWorker();

    @Test
    void taskDefName() {
        assertEquals("ss_search_unstructured", worker.getTaskDefName());
    }

    @Test
    void returnsTwoResults() {
        List<Map<String, String>> chunks = List.of(
                Map.of("id", "chunk-001", "type", "report", "content", "Some content")
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What are the growth drivers?",
                "textChunks", chunks
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void resultsHaveRequiredKeys() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (Map<String, Object> r : results) {
            assertNotNull(r.get("chunkId"));
            assertNotNull(r.get("score"));
            assertNotNull(r.get("snippet"));
            assertInstanceOf(String.class, r.get("chunkId"));
            assertInstanceOf(Double.class, r.get("score"));
            assertInstanceOf(String.class, r.get("snippet"));
        }
    }

    @Test
    void resultsHaveExpectedScores() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertEquals(0.91, results.get(0).get("score"));
        assertEquals(0.85, results.get(1).get("score"));
    }

    @Test
    void resultsContainExpectedChunkIds() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertEquals("chunk-001", results.get(0).get("chunkId"));
        assertEquals("chunk-003", results.get(1).get("chunkId"));
    }

    @Test
    void snippetsContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertTrue(((String) results.get(0).get("snippet")).contains("earnings"));
        assertTrue(((String) results.get(1).get("snippet")).contains("cloud services"));
    }

    @Test
    void handlesNullTextChunks() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
