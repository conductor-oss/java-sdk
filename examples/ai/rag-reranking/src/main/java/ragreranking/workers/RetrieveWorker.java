package ragreranking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves a broad set of candidates from a vector store.
 * Returns 6 candidates with bi-encoder similarity scores.
 * In production this would query a vector database (Pinecone, Weaviate, etc.).
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rerank_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        Object topKObj = task.getInputData().get("topK");
        int topK = 10;
        if (topKObj instanceof Number) {
            topK = ((Number) topKObj).intValue();
        } else if (topKObj instanceof String) {
            try {
                topK = Integer.parseInt((String) topKObj);
            } catch (NumberFormatException ignored) {
            }
        }

        System.out.println("  [retrieve] Fetching top-" + topK + " candidates from vector store");

        // Fixed deterministic candidates with bi-encoder scores
        List<Map<String, Object>> candidates = List.of(
                Map.of("id", "doc-A",
                        "text", "Cross-encoder models score query-document pairs jointly for higher accuracy than bi-encoders.",
                        "biEncoderScore", 0.91),
                Map.of("id", "doc-B",
                        "text", "Re-ranking improves RAG precision by filtering initial retrieval results with a second model.",
                        "biEncoderScore", 0.88),
                Map.of("id", "doc-C",
                        "text", "Bi-encoder models embed queries and documents independently, enabling fast vector search.",
                        "biEncoderScore", 0.86),
                Map.of("id", "doc-D",
                        "text", "The Conductor task queue manages worker polling and result acknowledgment.",
                        "biEncoderScore", 0.82),
                Map.of("id", "doc-E",
                        "text", "HNSW indexes provide logarithmic query time for approximate nearest neighbor search.",
                        "biEncoderScore", 0.79),
                Map.of("id", "doc-F",
                        "text", "Cohere Rerank API and cross-encoder/ms-marco-MiniLM-L-6-v2 are popular reranking models.",
                        "biEncoderScore", 0.76)
        );

        System.out.println("  [retrieve] Got " + candidates.size()
                + " candidates (bi-encoder scores: "
                + candidates.get(0).get("biEncoderScore") + " to "
                + candidates.get(candidates.size() - 1).get("biEncoderScore") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("candidates", candidates);
        result.getOutputData().put("candidateCount", candidates.size());
        return result;
    }
}
