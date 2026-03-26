package ragqdrant.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QdrantSearchWorkerTest {

    private final QdrantSearchWorker worker = new QdrantSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("qdrant_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreePoints() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, -0.2, 0.3),
                "collection", "knowledge",
                "limit", 3,
                "scoreThreshold", 0.7,
                "filter", new HashMap<>(Map.of("must", List.of(
                        new HashMap<>(Map.of("key", "status", "match", new HashMap<>(Map.of("value", "active")))))))
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) result.getOutputData().get("points");
        assertNotNull(points);
        assertEquals(3, points.size());

        // Verify first point
        assertEquals("a1b2c3d4", points.get(0).get("id"));
        assertEquals(3, points.get(0).get("version"));
        assertEquals(0.96, points.get(0).get("score"));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload1 = (Map<String, Object>) points.get(0).get("payload");
        assertEquals("Qdrant Overview", payload1.get("title"));
        assertEquals("Qdrant is a vector similarity search engine with extended filtering support.", payload1.get("content"));
        assertEquals("active", payload1.get("status"));

        // Verify second point
        assertEquals("e5f6a7b8", points.get(1).get("id"));
        assertEquals(2, points.get(1).get("version"));
        assertEquals(0.92, points.get(1).get("score"));

        // Verify third point
        assertEquals("c9d0e1f2", points.get(2).get("id"));
        assertEquals(1, points.get(2).get("version"));
        assertEquals(0.88, points.get(2).get("score"));
    }

    @Test
    void returnsCollectionInfo() {
        Task task = taskWith(new HashMap<>(Map.of(
                "collection", "knowledge",
                "limit", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertNotNull(collectionInfo);
        assertEquals("knowledge", collectionInfo.get("name"));
        assertEquals(1536, collectionInfo.get("vectorSize"));
        assertEquals("Cosine", collectionInfo.get("distance"));
        assertEquals(15680, collectionInfo.get("pointsCount"));
        assertEquals(20000, collectionInfo.get("indexingThreshold"));
    }

    @Test
    void defaultsCollectionWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of("limit", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertEquals("knowledge", collectionInfo.get("name"));
    }

    @Test
    void defaultsLimitAndThreshold() {
        Task task = taskWith(new HashMap<>(Map.of("collection", "docs")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) result.getOutputData().get("points");
        assertEquals(3, points.size());
    }

    @Test
    void collectionInfoReflectsInputName() {
        Task task = taskWith(new HashMap<>(Map.of("collection", "my_custom_collection")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertEquals("my_custom_collection", collectionInfo.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
