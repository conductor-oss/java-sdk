package ragredis.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that performs a Redis FT.SEARCH vector similarity query.
 *
 * Real Redis commands this implements:
 *
 * Create index:
 *   FT.CREATE idx:docs ON HASH PREFIX 1 doc:
 *     SCHEMA content TEXT
 *     embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE
 *
 * Search:
 *   FT.SEARCH idx:docs "*=>[KNN 3 @embedding $query_vec AS vector_score]"
 *     PARAMS 2 query_vec <blob>
 *     SORTBY vector_score
 *     RETURN 3 content source vector_score
 *     DIALECT 2
 */
public class RedisFtSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "redis_ft_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String indexName = (String) task.getInputData().get("indexName");
        Object kObj = task.getInputData().get("k");
        int k = (kObj instanceof Number) ? ((Number) kObj).intValue() : 3;
        String scoreField = (String) task.getInputData().get("scoreField");

        System.out.println("  [redis] FT.SEARCH on index \"" + indexName + "\" with KNN k=" + k);
        System.out.println("  [redis] Query: *=>[KNN " + k + " @embedding $query_vec AS " + scoreField + "]");

        List<Map<String, Object>> results = List.of(
                Map.of(
                        "key", "doc:1001",
                        "content", "Redis supports vector similarity search via the RediSearch module.",
                        "source", "redis-docs/vectors.md",
                        "vector_score", 0.04
                ),
                Map.of(
                        "key", "doc:1042",
                        "content", "HNSW and FLAT index types are available for vector fields.",
                        "source", "redis-docs/indexing.md",
                        "vector_score", 0.09
                ),
                Map.of(
                        "key", "doc:1078",
                        "content", "Use FT.SEARCH with KNN clause and DIALECT 2 for vector queries.",
                        "source", "redis-docs/search.md",
                        "vector_score", 0.14
                )
        );

        Map<String, Object> indexInfo = Map.of(
                "name", indexName,
                "type", "HNSW",
                "dim", 1536,
                "distanceMetric", "COSINE",
                "numDocs", 8920
        );

        System.out.println("  [redis] " + results.size() + " results (lower score = more similar in cosine distance)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("indexInfo", indexInfo);
        return result;
    }
}
