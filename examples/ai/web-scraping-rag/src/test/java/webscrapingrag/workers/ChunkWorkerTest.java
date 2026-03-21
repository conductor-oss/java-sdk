package webscrapingrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChunkWorkerTest {

    private final ChunkWorker worker = new ChunkWorker();

    @Test
    void taskDefName() {
        assertEquals("wsrag_chunk", worker.getTaskDefName());
    }

    @Test
    void chunksPages() {
        List<Map<String, Object>> pages = new ArrayList<>();
        pages.add(new HashMap<>(Map.of(
                "url", "https://example.com/page1",
                "title", "Page 1",
                "text", "First sentence. Second sentence. Third sentence",
                "wordCount", 6
        )));
        Task task = taskWith(new HashMap<>(Map.of("pages", pages)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertNotNull(chunks);
        // "First sentence. Second sentence" and "Third sentence"
        assertEquals(2, chunks.size());
        assertEquals(2, result.getOutputData().get("chunkCount"));
    }

    @Test
    void chunkIdsAreSequential() {
        List<Map<String, Object>> pages = new ArrayList<>();
        pages.add(new HashMap<>(Map.of(
                "url", "https://example.com/a",
                "title", "A",
                "text", "Sentence one. Sentence two",
                "wordCount", 4
        )));
        pages.add(new HashMap<>(Map.of(
                "url", "https://example.com/b",
                "title", "B",
                "text", "Sentence three. Sentence four",
                "wordCount", 4
        )));
        Task task = taskWith(new HashMap<>(Map.of("pages", pages)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertEquals("web-chunk-0", chunks.get(0).get("id"));
        assertEquals("web-chunk-1", chunks.get(1).get("id"));
    }

    @Test
    void chunkSourceMatchesPageUrl() {
        List<Map<String, Object>> pages = new ArrayList<>();
        pages.add(new HashMap<>(Map.of(
                "url", "https://example.com/test",
                "title", "Test",
                "text", "Hello world. Goodbye world",
                "wordCount", 4
        )));
        Task task = taskWith(new HashMap<>(Map.of("pages", pages)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertEquals("https://example.com/test", chunks.get(0).get("source"));
    }

    @Test
    void chunkTextJoinsSentencePairs() {
        List<Map<String, Object>> pages = new ArrayList<>();
        pages.add(new HashMap<>(Map.of(
                "url", "https://example.com/p",
                "title", "P",
                "text", "Alpha. Beta. Gamma. Delta",
                "wordCount", 4
        )));
        Task task = taskWith(new HashMap<>(Map.of("pages", pages)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertEquals(2, chunks.size());
        assertEquals("Alpha. Beta", chunks.get(0).get("text"));
        assertEquals("Gamma. Delta", chunks.get(1).get("text"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
