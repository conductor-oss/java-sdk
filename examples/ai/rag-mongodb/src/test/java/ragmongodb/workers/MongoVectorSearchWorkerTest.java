package ragmongodb.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MongoVectorSearchWorkerTest {

    private final MongoVectorSearchWorker worker = new MongoVectorSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("mongo_vector_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeDocuments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2, 0.3),
                "database", "knowledge_base",
                "collection", "articles",
                "numCandidates", 100,
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(3, documents.size());
    }

    @Test
    void documentFieldsAreCorrect() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "database", "test_db",
                "collection", "test_coll",
                "numCandidates", 50,
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        Map<String, Object> first = documents.get(0);
        assertEquals("64f1a2b3c4d5e6f7a8b9c0d1", first.get("_id"));
        assertEquals("Atlas Vector Search", first.get("title"));
        assertEquals("MongoDB Atlas Vector Search uses $vectorSearch aggregation pipeline stage.", first.get("content"));
        assertEquals(0.95, first.get("score"));

        Map<String, Object> second = documents.get(1);
        assertEquals("64f1a2b3c4d5e6f7a8b9c0d2", second.get("_id"));
        assertEquals("Vector Indexes", second.get("title"));
        assertEquals(0.91, second.get("score"));

        Map<String, Object> third = documents.get(2);
        assertEquals("64f1a2b3c4d5e6f7a8b9c0d3", third.get("_id"));
        assertEquals("Hybrid Queries", third.get("title"));
        assertEquals(0.87, third.get("score"));
    }

    @Test
    void metaContainsExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "database", "mydb",
                "collection", "mycoll",
                "numCandidates", 200,
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) result.getOutputData().get("meta");
        assertNotNull(meta);
        assertEquals("vector_index", meta.get("index"));
        assertEquals("embedding", meta.get("path"));
        assertEquals(200, meta.get("numCandidates"));
        assertEquals(24500, meta.get("totalDocs"));
    }

    @Test
    void scoresAreInDescendingOrder() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "database", "db",
                "collection", "coll",
                "numCandidates", 100,
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        double score1 = (double) documents.get(0).get("score");
        double score2 = (double) documents.get(1).get("score");
        double score3 = (double) documents.get(2).get("score");
        assertTrue(score1 > score2);
        assertTrue(score2 > score3);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
