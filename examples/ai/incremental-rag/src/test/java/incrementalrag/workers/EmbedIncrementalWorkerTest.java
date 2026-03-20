package incrementalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedIncrementalWorkerTest {

    private final EmbedIncrementalWorker worker = new EmbedIncrementalWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_embed_incremental", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesEmbeddings() {
        List<Map<String, Object>> docsToEmbed = List.of(
                Map.of("id", "doc-101", "text", "Updated content of doc-101", "action", "update"),
                Map.of("id", "doc-205", "text", "Content of doc-205", "action", "insert"),
                Map.of("id", "doc-307", "text", "Updated content of doc-307", "action", "update"),
                Map.of("id", "doc-412", "text", "Content of doc-412", "action", "insert")
        );

        Task task = taskWith(Map.of("docsToEmbed", docsToEmbed));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> embeddings = (List<Map<String, Object>>) result.getOutputData().get("embeddings");
        assertNotNull(embeddings);
        assertEquals(4, embeddings.size());

        // Check first embedding
        assertEquals("doc-101", embeddings.get(0).get("id"));
        assertEquals("update", embeddings.get(0).get("action"));
        List<Double> vector = (List<Double>) embeddings.get(0).get("vector");
        assertNotNull(vector);
        assertEquals(5, vector.size());
        assertEquals(0.12, vector.get(0));
        assertEquals(-0.34, vector.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDocIdsAndCount() {
        List<Map<String, Object>> docsToEmbed = List.of(
                Map.of("id", "doc-101", "text", "text1", "action", "update"),
                Map.of("id", "doc-205", "text", "text2", "action", "insert")
        );

        Task task = taskWith(Map.of("docsToEmbed", docsToEmbed));
        TaskResult result = worker.execute(task);

        List<String> docIds = (List<String>) result.getOutputData().get("docIds");
        assertNotNull(docIds);
        assertEquals(2, docIds.size());
        assertEquals("doc-101", docIds.get(0));
        assertEquals("doc-205", docIds.get(1));
        assertEquals(2, result.getOutputData().get("embeddedCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDeterministicVectors() {
        List<Map<String, Object>> docsToEmbed = List.of(
                Map.of("id", "doc-101", "text", "text1", "action", "insert"),
                Map.of("id", "doc-205", "text", "text2", "action", "insert")
        );

        Task task = taskWith(Map.of("docsToEmbed", docsToEmbed));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> embeddings = (List<Map<String, Object>>) result.getOutputData().get("embeddings");
        List<Double> vector1 = (List<Double>) embeddings.get(0).get("vector");
        List<Double> vector2 = (List<Double>) embeddings.get(1).get("vector");

        // Both should use the same deterministic vector
        assertEquals(vector1, vector2);
        assertEquals(List.of(0.12, -0.34, 0.56, 0.78, -0.91), vector1);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
