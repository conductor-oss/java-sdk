package ragelasticsearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EsKnnSearchWorkerTest {

    private final EsKnnSearchWorker worker = new EsKnnSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("es_knn_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeHitsAndStats() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, -0.2, 0.3, -0.4, 0.5, -0.6, 0.7, -0.8),
                "index", "knowledge-docs",
                "k", 3,
                "numCandidates", 50
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) result.getOutputData().get("hits");
        assertNotNull(hits);
        assertEquals(3, hits.size());

        // Verify first hit
        assertEquals("es-doc-1", hits.get(0).get("_id"));
        assertEquals(0.97, hits.get(0).get("_score"));
        @SuppressWarnings("unchecked")
        Map<String, Object> source1 = (Map<String, Object>) hits.get(0).get("_source");
        assertEquals("ES Vector Search", source1.get("title"));
        assertTrue(((String) source1.get("content")).contains("dense vector fields"));
        assertEquals("/docs/knn", source1.get("url"));

        // Verify second hit
        assertEquals("es-doc-2", hits.get(1).get("_id"));
        assertEquals(0.92, hits.get(1).get("_score"));
        @SuppressWarnings("unchecked")
        Map<String, Object> source2 = (Map<String, Object>) hits.get(1).get("_source");
        assertEquals("Index Mapping", source2.get("title"));
        assertEquals("/docs/mapping", source2.get("url"));

        // Verify third hit
        assertEquals("es-doc-3", hits.get(2).get("_id"));
        assertEquals(0.88, hits.get(2).get("_score"));
        @SuppressWarnings("unchecked")
        Map<String, Object> source3 = (Map<String, Object>) hits.get(2).get("_source");
        assertEquals("Hybrid Search", source3.get("title"));
        assertEquals("/docs/hybrid", source3.get("url"));

        // Verify stats
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertNotNull(stats);
        assertEquals(12, stats.get("took"));
        assertEquals(3, stats.get("totalHits"));
        assertEquals(0.97, stats.get("maxScore"));

        @SuppressWarnings("unchecked")
        Map<String, Object> shards = (Map<String, Object>) stats.get("shards");
        assertEquals(5, shards.get("total"));
        assertEquals(5, shards.get("successful"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "index", "test-index",
                "k", 3,
                "numCandidates", 50
        )));
        Task task2 = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.9, 0.8),
                "index", "test-index",
                "k", 3,
                "numCandidates", 50
        )));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("hits"), result2.getOutputData().get("hits"));
        assertEquals(result1.getOutputData().get("stats"), result2.getOutputData().get("stats"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
