package raglangchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitTextWorkerTest {

    private final SplitTextWorker worker = new SplitTextWorker();

    @Test
    void taskDefName() {
        assertEquals("lc_split_text", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void splitsDocumentsIntoChunks() {
        List<Map<String, Object>> documents = List.of(
                Map.of("pageContent", "First sentence. Second sentence.",
                        "metadata", Map.of("source", "test", "page", 1)),
                Map.of("pageContent", "Third sentence.",
                        "metadata", Map.of("source", "test", "page", 2))
        );

        Task task = taskWith(Map.of("documents", documents, "chunkSize", 200, "chunkOverlap", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");
        assertNotNull(chunks);
        assertTrue(chunks.size() >= 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void chunksHaveTextAndMetadata() {
        List<Map<String, Object>> documents = List.of(
                Map.of("pageContent", "Test content.",
                        "metadata", Map.of("source", "test", "page", 1))
        );

        Task task = taskWith(Map.of("documents", documents));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) result.getOutputData().get("chunks");
        Map<String, Object> firstChunk = chunks.get(0);
        assertNotNull(firstChunk.get("text"));
        assertTrue(firstChunk.get("text") instanceof String);

        Map<String, Object> metadata = (Map<String, Object>) firstChunk.get("metadata");
        assertNotNull(metadata);
        assertEquals(0, metadata.get("chunkIndex"));
    }

    @Test
    void returnsChunkCountAndSplitter() {
        List<Map<String, Object>> documents = List.of(
                Map.of("pageContent", "Some content.",
                        "metadata", Map.of("source", "test", "page", 1))
        );

        Task task = taskWith(Map.of("documents", documents));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("chunkCount"));
        assertEquals("RecursiveCharacterTextSplitter", result.getOutputData().get("splitter"));
    }

    @Test
    void handlesDefaultChunkParameters() {
        List<Map<String, Object>> documents = List.of(
                Map.of("pageContent", "Content here.",
                        "metadata", Map.of("source", "test", "page", 1))
        );

        Task task = taskWith(Map.of("documents", documents));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
