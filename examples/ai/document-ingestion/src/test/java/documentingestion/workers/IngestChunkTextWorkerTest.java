package documentingestion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestChunkTextWorkerTest {

    private final IngestChunkTextWorker worker = new IngestChunkTextWorker();

    @Test
    void taskDefName() {
        assertEquals("ingest_chunk_text", worker.getTaskDefName());
    }

    @Test
    void chunksTextWithDefaultSize() {
        String text = "word ".repeat(100).trim();
        Task task = taskWith(new HashMap<>(Map.of("text", text)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("chunks"));
        // 100 words with default chunkSize=200 should produce 1 chunk
        assertEquals(1, result.getOutputData().get("chunkCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chunksTextWithCustomSize() {
        String text = "word ".repeat(50).trim();
        Task task = taskWith(new HashMap<>(Map.of("text", text, "chunkSize", "20", "chunkOverlap", "5")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertNotNull(chunks);
        assertTrue(chunks.size() > 1);
        assertEquals(chunks.size(), result.getOutputData().get("chunkCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chunkHasCorrectFields() {
        String text = "alpha beta gamma delta epsilon";
        Task task = taskWith(new HashMap<>(Map.of("text", text, "chunkSize", "3", "chunkOverlap", "1")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        Map<String, Object> firstChunk = chunks.get(0);
        assertEquals("chunk-0", firstChunk.get("id"));
        assertEquals(3, firstChunk.get("wordCount"));
        assertEquals(0, firstChunk.get("startOffset"));
        assertEquals("alpha beta gamma", firstChunk.get("text"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void overlappingChunksShareWords() {
        // 10 words, chunkSize=5, overlap=2 -> step=3
        // chunk-0: words 0..4, chunk-1: words 3..7, chunk-2: words 6..9
        String text = "a b c d e f g h i j";
        Task task = taskWith(new HashMap<>(Map.of("text", text, "chunkSize", "5", "chunkOverlap", "2")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertTrue(chunks.size() >= 2);

        // First chunk starts at offset 0
        assertEquals(0, chunks.get(0).get("startOffset"));
        // Second chunk starts at offset 3 (5-2=3)
        assertEquals(3, chunks.get(1).get("startOffset"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chunkIdsAreSequential() {
        String text = "word ".repeat(30).trim();
        Task task = taskWith(new HashMap<>(Map.of("text", text, "chunkSize", "10", "chunkOverlap", "2")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOutputData().get("chunks");
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals("chunk-" + i, chunks.get(i).get("id"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
