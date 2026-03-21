package raglangchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedChunksWorkerTest {

    private final EmbedChunksWorker worker = new EmbedChunksWorker();

    @Test
    void taskDefName() {
        assertEquals("lc_embed_chunks", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesEmbeddingsForChunks() {
        List<Map<String, Object>> chunks = List.of(
                Map.of("text", "First chunk.", "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "Second chunk.", "metadata", Map.of("chunkIndex", 1))
        );

        Task task = taskWith(Map.of("chunks", chunks));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> embeddings =
                (List<Map<String, Object>>) result.getOutputData().get("embeddings");
        assertNotNull(embeddings);
        assertEquals(2, embeddings.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void embeddingsHaveRequiredFields() {
        List<Map<String, Object>> chunks = List.of(
                Map.of("text", "Test chunk.", "metadata", Map.of("chunkIndex", 0))
        );

        Task task = taskWith(Map.of("chunks", chunks));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> embeddings =
                (List<Map<String, Object>>) result.getOutputData().get("embeddings");
        Map<String, Object> emb = embeddings.get(0);

        assertEquals("emb-0", emb.get("id"));
        assertEquals("Test chunk.", emb.get("text"));
        assertNotNull(emb.get("metadata"));

        List<Double> vector = (List<Double>) emb.get("vector");
        assertNotNull(vector);
        assertEquals(4, vector.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void vectorsAreDeterministic() {
        List<Map<String, Object>> chunks = List.of(
                Map.of("text", "Deterministic test.", "metadata", Map.of("chunkIndex", 0))
        );

        Task task1 = taskWith(Map.of("chunks", chunks));
        TaskResult result1 = worker.execute(task1);
        List<Map<String, Object>> emb1 =
                (List<Map<String, Object>>) result1.getOutputData().get("embeddings");

        Task task2 = taskWith(Map.of("chunks", chunks));
        TaskResult result2 = worker.execute(task2);
        List<Map<String, Object>> emb2 =
                (List<Map<String, Object>>) result2.getOutputData().get("embeddings");

        assertEquals(emb1.get(0).get("vector"), emb2.get(0).get("vector"));
    }

    @Test
    void returnsEmbeddingCountAndModel() {
        List<Map<String, Object>> chunks = List.of(
                Map.of("text", "Chunk one.", "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "Chunk two.", "metadata", Map.of("chunkIndex", 1)),
                Map.of("text", "Chunk three.", "metadata", Map.of("chunkIndex", 2))
        );

        Task task = taskWith(Map.of("chunks", chunks));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("embeddingCount"));
        assertEquals("text-embedding-3-small", result.getOutputData().get("model"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
