package raglangchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveWorkerTest {

    private final RetrieveWorker worker = new RetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("lc_retrieve", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retrievesTopKDocuments() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("text", "First.", "vector", List.of(0.1, 0.2, 0.3, 0.4), "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "Second.", "vector", List.of(0.2, 0.3, 0.4, 0.5), "metadata", Map.of("chunkIndex", 1)),
                Map.of("text", "Third.", "vector", List.of(0.3, 0.4, 0.5, 0.6), "metadata", Map.of("chunkIndex", 2)),
                Map.of("text", "Fourth.", "vector", List.of(0.4, 0.5, 0.6, 0.7), "metadata", Map.of("chunkIndex", 3))
        );

        Task task = taskWith(Map.of(
                "question", "What is RAG?",
                "embeddings", embeddings,
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> retrievedDocs =
                (List<Map<String, Object>>) result.getOutputData().get("retrievedDocs");
        assertNotNull(retrievedDocs);
        assertEquals(3, retrievedDocs.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignsDecreasingScores() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("text", "A.", "vector", List.of(0.1, 0.2, 0.3, 0.4), "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "B.", "vector", List.of(0.2, 0.3, 0.4, 0.5), "metadata", Map.of("chunkIndex", 1)),
                Map.of("text", "C.", "vector", List.of(0.3, 0.4, 0.5, 0.6), "metadata", Map.of("chunkIndex", 2))
        );

        Task task = taskWith(Map.of(
                "question", "Test?",
                "embeddings", embeddings,
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> docs =
                (List<Map<String, Object>>) result.getOutputData().get("retrievedDocs");
        assertEquals(0.95, docs.get(0).get("score"));
        assertEquals(0.87, docs.get(1).get("score"));
        assertEquals(0.79, docs.get(2).get("score"));
    }

    @Test
    void returnsRetrieverType() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("text", "Doc.", "vector", List.of(0.1, 0.2, 0.3, 0.4), "metadata", Map.of("chunkIndex", 0))
        );

        Task task = taskWith(Map.of(
                "question", "Test?",
                "embeddings", embeddings
        ));
        TaskResult result = worker.execute(task);

        assertEquals("FAISS", result.getOutputData().get("retriever"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void defaultsTopKToThree() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("text", "A.", "vector", List.of(0.1, 0.2, 0.3, 0.4), "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "B.", "vector", List.of(0.2, 0.3, 0.4, 0.5), "metadata", Map.of("chunkIndex", 1)),
                Map.of("text", "C.", "vector", List.of(0.3, 0.4, 0.5, 0.6), "metadata", Map.of("chunkIndex", 2)),
                Map.of("text", "D.", "vector", List.of(0.4, 0.5, 0.6, 0.7), "metadata", Map.of("chunkIndex", 3)),
                Map.of("text", "E.", "vector", List.of(0.5, 0.6, 0.7, 0.8), "metadata", Map.of("chunkIndex", 4))
        );

        Task task = taskWith(Map.of(
                "question", "Test?",
                "embeddings", embeddings
        ));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> docs =
                (List<Map<String, Object>>) result.getOutputData().get("retrievedDocs");
        assertEquals(3, docs.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
