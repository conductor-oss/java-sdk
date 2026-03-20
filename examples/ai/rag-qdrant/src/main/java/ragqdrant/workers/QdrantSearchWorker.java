package ragqdrant.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that searches a Qdrant collection with an embedding vector.
 * Returns default points with payload data for deterministic testing.
 *
 * In production this would POST to /collections/{collection}/points/search
 * with vector, limit, score_threshold, filter, and with_payload parameters.
 */
public class QdrantSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qdrant_search";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        Object limitObj = task.getInputData().get("limit");
        Object scoreThresholdObj = task.getInputData().get("scoreThreshold");
        Object filterObj = task.getInputData().get("filter");

        if (collection == null || collection.isBlank()) {
            collection = "knowledge";
        }
        int limit = 3;
        if (limitObj instanceof Number) {
            limit = ((Number) limitObj).intValue();
        }
        double scoreThreshold = 0.7;
        if (scoreThresholdObj instanceof Number) {
            scoreThreshold = ((Number) scoreThresholdObj).doubleValue();
        }

        System.out.println("  [qdrant] Collection: \"" + collection
                + "\", limit=" + limit + ", threshold=" + scoreThreshold);
        System.out.println("  [qdrant] Filter: " + filterObj);

        // Build fixed points
        List<Map<String, Object>> points = new ArrayList<>();

        Map<String, Object> point1 = new HashMap<>();
        point1.put("id", "a1b2c3d4");
        point1.put("version", 3);
        point1.put("score", 0.96);
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("title", "Qdrant Overview");
        payload1.put("content", "Qdrant is a vector similarity search engine with extended filtering support.");
        payload1.put("status", "active");
        point1.put("payload", payload1);
        points.add(point1);

        Map<String, Object> point2 = new HashMap<>();
        point2.put("id", "e5f6a7b8");
        point2.put("version", 2);
        point2.put("score", 0.92);
        Map<String, Object> payload2 = new HashMap<>();
        payload2.put("title", "Payload Filtering");
        payload2.put("content", "Qdrant supports rich filtering on payload fields during vector search.");
        payload2.put("status", "active");
        point2.put("payload", payload2);
        points.add(point2);

        Map<String, Object> point3 = new HashMap<>();
        point3.put("id", "c9d0e1f2");
        point3.put("version", 1);
        point3.put("score", 0.88);
        Map<String, Object> payload3 = new HashMap<>();
        payload3.put("title", "Collections");
        payload3.put("content", "Collections in Qdrant store points with vectors and payload data, configurable per collection.");
        payload3.put("status", "active");
        point3.put("payload", payload3);
        points.add(point3);

        // Build collection info
        Map<String, Object> collectionInfo = new HashMap<>();
        collectionInfo.put("name", collection);
        collectionInfo.put("vectorSize", 1536);
        collectionInfo.put("distance", "Cosine");
        collectionInfo.put("pointsCount", 15680);
        collectionInfo.put("indexingThreshold", 20000);

        System.out.println("  [qdrant] " + points.size() + " points found from "
                + collectionInfo.get("pointsCount") + " total");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("points", points);
        result.getOutputData().put("collectionInfo", collectionInfo);
        return result;
    }
}
