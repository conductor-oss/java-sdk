package ragelasticsearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that runs an Elasticsearch knn vector search.
 *
 * In production, this would issue a POST to /{index}/_search with a knn query:
 * {
 *   "knn": {
 *     "field": "content_embedding",
 *     "query_vector": [...],
 *     "k": 3,
 *     "num_candidates": 50
 *   },
 *   "_source": ["title", "content", "url"]
 * }
 *
 * Index mapping for the dense_vector field:
 * "content_embedding": { "type": "dense_vector", "dims": 1536, "index": true, "similarity": "cosine" }
 */
public class EsKnnSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_knn_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String index = (String) task.getInputData().get("index");
        Object k = task.getInputData().get("k");
        Object numCandidates = task.getInputData().get("numCandidates");

        System.out.println("  [elasticsearch] Index: \"" + index + "\", k=" + k
                + ", num_candidates=" + numCandidates);
        System.out.println("  [elasticsearch] Field: \"content_embedding\" (dense_vector, cosine similarity)");

        List<Map<String, Object>> hits = List.of(
                Map.of(
                        "_id", "es-doc-1",
                        "_score", 0.97,
                        "_source", Map.of(
                                "title", "ES Vector Search",
                                "content", "Elasticsearch supports dense vector fields with knn search for semantic similarity.",
                                "url", "/docs/knn"
                        )
                ),
                Map.of(
                        "_id", "es-doc-2",
                        "_score", 0.92,
                        "_source", Map.of(
                                "title", "Index Mapping",
                                "content", "Configure dense_vector fields with dims, index, and similarity parameters.",
                                "url", "/docs/mapping"
                        )
                ),
                Map.of(
                        "_id", "es-doc-3",
                        "_score", 0.88,
                        "_source", Map.of(
                                "title", "Hybrid Search",
                                "content", "Combine knn vector search with BM25 text scoring for hybrid retrieval.",
                                "url", "/docs/hybrid"
                        )
                )
        );

        Map<String, Object> stats = Map.of(
                "took", 12,
                "totalHits", 3,
                "maxScore", 0.97,
                "shards", Map.of("total", 5, "successful", 5)
        );

        System.out.println("  [elasticsearch] " + hits.size() + " hits in " + stats.get("took")
                + "ms across " + ((Map<?, ?>) stats.get("shards")).get("total") + " shards");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hits", hits);
        result.getOutputData().put("stats", stats);
        return result;
    }
}
