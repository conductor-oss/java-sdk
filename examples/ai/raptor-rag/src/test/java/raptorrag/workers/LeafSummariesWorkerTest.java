package raptorrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeafSummariesWorkerTest {

    private final LeafSummariesWorker worker = new LeafSummariesWorker();

    @Test
    void taskDefName() {
        assertEquals("rp_leaf_summaries", worker.getTaskDefName());
    }

    @Test
    void returnsFourLeafSummaries() {
        List<Map<String, Object>> chunks = List.of(
                Map.of("id", "chunk-1", "text", "Some text", "tokens", 10),
                Map.of("id", "chunk-2", "text", "More text", "tokens", 12)
        );

        Task task = taskWith(new HashMap<>(Map.of("chunks", chunks)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("leafSummaries");
        assertNotNull(leafSummaries);
        assertEquals(4, leafSummaries.size());
        assertEquals(4, result.getOutputData().get("leafCount"));
    }

    @Test
    void leafSummariesHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("chunks", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("leafSummaries");

        for (Map<String, Object> leaf : leafSummaries) {
            assertNotNull(leaf.get("id"));
            assertNotNull(leaf.get("text"));
            assertNotNull(leaf.get("chunkIds"));
            assertNotNull(leaf.get("level"));
            assertInstanceOf(String.class, leaf.get("id"));
            assertInstanceOf(String.class, leaf.get("text"));
            assertInstanceOf(List.class, leaf.get("chunkIds"));
            assertEquals(0, leaf.get("level"));
        }
    }

    @Test
    void leafSummariesHaveExpectedIds() {
        Task task = taskWith(new HashMap<>(Map.of("chunks", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("leafSummaries");

        assertEquals("leaf-1", leafSummaries.get(0).get("id"));
        assertEquals("leaf-2", leafSummaries.get(1).get("id"));
        assertEquals("leaf-3", leafSummaries.get(2).get("id"));
        assertEquals("leaf-4", leafSummaries.get(3).get("id"));
    }

    @Test
    void leafSummariesReferenceChunkIds() {
        Task task = taskWith(new HashMap<>(Map.of("chunks", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("leafSummaries");

        @SuppressWarnings("unchecked")
        List<String> chunkIds = (List<String>) leafSummaries.get(0).get("chunkIds");
        assertTrue(chunkIds.contains("chunk-1"));
        assertTrue(chunkIds.contains("chunk-2"));
    }

    @Test
    void handlesNullChunks() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("leafSummaries");
        assertNotNull(leafSummaries);
        assertEquals(4, leafSummaries.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
