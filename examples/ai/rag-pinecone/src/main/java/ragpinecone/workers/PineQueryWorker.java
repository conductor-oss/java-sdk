package ragpinecone.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that queries a Pinecone index with an embedding vector.
 * Returns default matches for deterministic testing.
 */
public class PineQueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pine_query";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object embeddingObj = task.getInputData().get("embedding");
        String namespace = (String) task.getInputData().get("namespace");
        Object topKObj = task.getInputData().get("topK");
        Object filterObj = task.getInputData().get("filter");

        if (namespace == null || namespace.isBlank()) {
            namespace = "default";
        }
        int topK = 3;
        if (topKObj instanceof Number) {
            topK = ((Number) topKObj).intValue();
        }

        System.out.println("  [pine_query worker] Querying namespace '" + namespace
                + "' with topK=" + topK);

        // Build fixed matches
        List<Map<String, Object>> matches = new ArrayList<>();

        Map<String, Object> match1 = new HashMap<>();
        match1.put("id", "vec-001");
        match1.put("score", 0.96);
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("text", "Pinecone is a managed vector database for ML applications.");
        meta1.put("category", "technical");
        match1.put("metadata", meta1);
        matches.add(match1);

        Map<String, Object> match2 = new HashMap<>();
        match2.put("id", "vec-002");
        match2.put("score", 0.91);
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("text", "Serverless indexes scale automatically based on usage.");
        meta2.put("category", "technical");
        match2.put("metadata", meta2);
        matches.add(match2);

        Map<String, Object> match3 = new HashMap<>();
        match3.put("id", "vec-003");
        match3.put("score", 0.87);
        Map<String, Object> meta3 = new HashMap<>();
        meta3.put("text", "Namespaces partition data within a single index for multi-tenancy.");
        meta3.put("category", "technical");
        match3.put("metadata", meta3);
        matches.add(match3);

        // Build stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("indexFullness", 0.23);
        stats.put("dimension", 1536);
        Map<String, Object> namespaces = new HashMap<>();
        namespaces.put("default", Map.of("vectorCount", 10245));
        namespaces.put("staging", Map.of("vectorCount", 3120));
        stats.put("namespaces", namespaces);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("matches", matches);
        result.getOutputData().put("namespace", namespace);
        result.getOutputData().put("stats", stats);
        return result;
    }
}
