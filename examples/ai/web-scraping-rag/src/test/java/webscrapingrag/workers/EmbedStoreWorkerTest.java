package webscrapingrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedStoreWorkerTest {

    private final EmbedStoreWorker worker = new EmbedStoreWorker();

    @Test
    void taskDefName() {
        assertEquals("wsrag_embed_store", worker.getTaskDefName());
    }

    @Test
    void storesChunksAndReturnsIds() {
        List<Map<String, Object>> chunks = new ArrayList<>();
        chunks.add(new HashMap<>(Map.of("id", "web-chunk-0", "text", "Some text", "source", "https://example.com")));
        chunks.add(new HashMap<>(Map.of("id", "web-chunk-1", "text", "More text", "source", "https://example.com")));

        Task task = taskWith(new HashMap<>(Map.of("chunks", chunks)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> storedIds = (List<String>) result.getOutputData().get("storedIds");
        assertNotNull(storedIds);
        assertEquals(2, storedIds.size());
        assertEquals("web-chunk-0", storedIds.get(0));
        assertEquals("web-chunk-1", storedIds.get(1));
    }

    @Test
    void countMatchesChunkSize() {
        List<Map<String, Object>> chunks = new ArrayList<>();
        chunks.add(new HashMap<>(Map.of("id", "c-0", "text", "A", "source", "u")));
        chunks.add(new HashMap<>(Map.of("id", "c-1", "text", "B", "source", "u")));
        chunks.add(new HashMap<>(Map.of("id", "c-2", "text", "C", "source", "u")));

        Task task = taskWith(new HashMap<>(Map.of("chunks", chunks)));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyChunks() {
        List<Map<String, Object>> chunks = new ArrayList<>();

        Task task = taskWith(new HashMap<>(Map.of("chunks", chunks)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));

        @SuppressWarnings("unchecked")
        List<String> storedIds = (List<String>) result.getOutputData().get("storedIds");
        assertTrue(storedIds.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
