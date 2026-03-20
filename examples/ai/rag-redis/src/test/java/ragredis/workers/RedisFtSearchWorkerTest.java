package ragredis.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisFtSearchWorkerTest {

    private final RedisFtSearchWorker worker = new RedisFtSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("redis_ft_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2, 0.3),
                "indexName", "idx:knowledge",
                "k", 3,
                "scoreField", "vector_score"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void resultKeysAndContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "indexName", "idx:test",
                "k", 3,
                "scoreField", "vector_score"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertEquals("doc:1001", results.get(0).get("key"));
        assertEquals("Redis supports vector similarity search via the RediSearch module.",
                results.get(0).get("content"));
        assertEquals("redis-docs/vectors.md", results.get(0).get("source"));
        assertEquals(0.04, results.get(0).get("vector_score"));

        assertEquals("doc:1042", results.get(1).get("key"));
        assertEquals(0.09, results.get(1).get("vector_score"));

        assertEquals("doc:1078", results.get(2).get("key"));
        assertEquals(0.14, results.get(2).get("vector_score"));
    }

    @Test
    void indexInfoReflectsInput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.5),
                "indexName", "idx:myindex",
                "k", 5,
                "scoreField", "score"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> indexInfo = (Map<String, Object>) result.getOutputData().get("indexInfo");
        assertNotNull(indexInfo);
        assertEquals("idx:myindex", indexInfo.get("name"));
        assertEquals("HNSW", indexInfo.get("type"));
        assertEquals(1536, indexInfo.get("dim"));
        assertEquals("COSINE", indexInfo.get("distanceMetric"));
        assertEquals(8920, indexInfo.get("numDocs"));
    }

    @Test
    void scoresAreOrderedAscending() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "indexName", "idx:test",
                "k", 3,
                "scoreField", "vector_score"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        double prev = -1;
        for (Map<String, Object> r : results) {
            double score = (double) r.get("vector_score");
            assertTrue(score > prev, "Scores should be ascending (lower = more similar)");
            prev = score;
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
