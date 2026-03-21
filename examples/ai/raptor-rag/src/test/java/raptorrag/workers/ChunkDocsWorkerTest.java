package raptorrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChunkDocsWorkerTest {

    private final ChunkDocsWorker worker = new ChunkDocsWorker();

    @Test
    void taskDefName() {
        assertEquals("rp_chunk_docs", worker.getTaskDefName());
    }

    @Test
    void returnsSixChunks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "documentText", "Some document content about Conductor.")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertNotNull(chunks);
        assertEquals(6, chunks.size());
        assertEquals(6, result.getOutputData().get("chunkCount"));
    }

    @Test
    void chunksHaveIdTextAndTokens() {
        Task task = taskWith(new HashMap<>(Map.of(
                "documentText", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");

        for (Map<String, Object> chunk : chunks) {
            assertNotNull(chunk.get("id"));
            assertNotNull(chunk.get("text"));
            assertNotNull(chunk.get("tokens"));
            assertInstanceOf(String.class, chunk.get("id"));
            assertInstanceOf(String.class, chunk.get("text"));
            assertInstanceOf(Integer.class, chunk.get("tokens"));
        }
    }

    @Test
    void chunksHaveExpectedIds() {
        Task task = taskWith(new HashMap<>(Map.of(
                "documentText", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");

        assertEquals("chunk-1", chunks.get(0).get("id"));
        assertEquals("chunk-2", chunks.get(1).get("id"));
        assertEquals("chunk-3", chunks.get(2).get("id"));
        assertEquals("chunk-4", chunks.get(3).get("id"));
        assertEquals("chunk-5", chunks.get(4).get("id"));
        assertEquals("chunk-6", chunks.get(5).get("id"));
    }

    @Test
    void chunksContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "documentText", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");

        assertTrue(((String) chunks.get(0).get("text")).contains("orchestration"));
        assertTrue(((String) chunks.get(2).get("text")).contains("Java"));
        assertTrue(((String) chunks.get(4).get("text")).contains("versioning"));
    }

    @Test
    void handlesNullDocumentText() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertNotNull(chunks);
        assertEquals(6, chunks.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
