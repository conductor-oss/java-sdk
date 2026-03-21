package ragpgvector.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PgvecQueryWorkerTest {

    private final PgvecQueryWorker worker = new PgvecQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("pgvec_query", worker.getTaskDefName());
    }

    @Test
    void cosineDistanceUsesCorrectOperator() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "table", "knowledge_embeddings",
                "limit", 3,
                "distanceMetric", "cosine"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String sqlQuery = (String) result.getOutputData().get("sqlQuery");
        assertNotNull(sqlQuery);
        assertTrue(sqlQuery.contains("<=>"), "cosine should use <=> operator");
        assertTrue(sqlQuery.contains("knowledge_embeddings"));
        assertTrue(sqlQuery.contains("LIMIT 3"));
    }

    @Test
    void l2DistanceUsesCorrectOperator() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "table", "docs",
                "limit", 5,
                "distanceMetric", "l2"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String sqlQuery = (String) result.getOutputData().get("sqlQuery");
        assertTrue(sqlQuery.contains("<->"), "l2 should use <-> operator");
        assertTrue(sqlQuery.contains("LIMIT 5"));
    }

    @Test
    void innerProductUsesCorrectOperator() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "table", "docs",
                "limit", 3,
                "distanceMetric", "ip"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String sqlQuery = (String) result.getOutputData().get("sqlQuery");
        assertTrue(sqlQuery.contains("<#>"), "ip should use <#> operator");
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsThreeRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "table", "knowledge_embeddings",
                "limit", 3,
                "distanceMetric", "cosine"
        )));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertNotNull(rows);
        assertEquals(3, rows.size());

        // Check first row
        assertEquals(1, rows.get(0).get("id"));
        assertEquals("pgvector adds vector similarity search to PostgreSQL with indexing support.",
                rows.get(0).get("content"));
        assertEquals("pg_docs.md", rows.get(0).get("source"));
        assertEquals(0.94, rows.get(0).get("similarity"));

        // Check second row
        assertEquals(2, rows.get(1).get("id"));
        assertEquals("indexing.md", rows.get(1).get("source"));
        assertEquals(0.89, rows.get(1).get("similarity"));

        // Check third row
        assertEquals(3, rows.get(2).get("id"));
        assertEquals("operators.md", rows.get(2).get("source"));
        assertEquals(0.86, rows.get(2).get("similarity"));
    }

    @Test
    void defaultsToDocumentsTableWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "limit", 3,
                "distanceMetric", "cosine"
        )));
        TaskResult result = worker.execute(task);

        String sqlQuery = (String) result.getOutputData().get("sqlQuery");
        assertTrue(sqlQuery.contains("FROM documents "));
    }

    @Test
    void defaultsToCosineWhenMetricMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "table", "test_table",
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        String sqlQuery = (String) result.getOutputData().get("sqlQuery");
        assertTrue(sqlQuery.contains("<=>"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
