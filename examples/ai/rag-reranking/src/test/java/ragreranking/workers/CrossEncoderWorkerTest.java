package ragreranking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrossEncoderWorkerTest {

    private final CrossEncoderWorker worker = new CrossEncoderWorker();

    @Test
    void taskDefName() {
        assertEquals("rerank_crossencoder", worker.getTaskDefName());
    }

    @Test
    void reranksAndReturnsTopN() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does re-ranking improve RAG accuracy?",
                "candidates", buildCandidates(),
                "topN", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reranked =
                (List<Map<String, Object>>) result.getOutputData().get("reranked");
        assertNotNull(reranked);
        assertEquals(3, reranked.size());
    }

    @Test
    void rerankedOrderDocBFirst() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does re-ranking improve RAG accuracy?",
                "candidates", buildCandidates(),
                "topN", 6
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reranked =
                (List<Map<String, Object>>) result.getOutputData().get("reranked");

        // After re-ranking: doc-B(0.97), doc-A(0.94), doc-F(0.91), doc-C(0.72), doc-E(0.45), doc-D(0.31)
        assertEquals("doc-B", reranked.get(0).get("id"));
        assertEquals(0.97, reranked.get(0).get("crossEncoderScore"));

        assertEquals("doc-A", reranked.get(1).get("id"));
        assertEquals(0.94, reranked.get(1).get("crossEncoderScore"));

        assertEquals("doc-F", reranked.get(2).get("id"));
        assertEquals(0.91, reranked.get(2).get("crossEncoderScore"));

        assertEquals("doc-C", reranked.get(3).get("id"));
        assertEquals(0.72, reranked.get(3).get("crossEncoderScore"));

        assertEquals("doc-E", reranked.get(4).get("id"));
        assertEquals(0.45, reranked.get(4).get("crossEncoderScore"));

        assertEquals("doc-D", reranked.get(5).get("id"));
        assertEquals(0.31, reranked.get(5).get("crossEncoderScore"));
    }

    @Test
    void returnsRerankedCount() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "candidates", buildCandidates(),
                "topN", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("rerankedCount"));
    }

    @Test
    void returnsModelName() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "candidates", buildCandidates(),
                "topN", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals("cross-encoder/ms-marco-MiniLM-L-6-v2", result.getOutputData().get("model"));
    }

    @Test
    void preservesBiEncoderScoreInOutput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "candidates", buildCandidates(),
                "topN", 6
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reranked =
                (List<Map<String, Object>>) result.getOutputData().get("reranked");

        // doc-B is first after reranking; its original biEncoderScore was 0.88
        Number biScore = (Number) reranked.get(0).get("biEncoderScore");
        assertEquals(0.88, biScore.doubleValue(), 0.001);
    }

    @Test
    void handlesNullCandidates() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "topN", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reranked =
                (List<Map<String, Object>>) result.getOutputData().get("reranked");
        assertNotNull(reranked);
        assertEquals(0, reranked.size());
    }

    @Test
    void parsesStringTopN() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "candidates", buildCandidates(),
                "topN", "3"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reranked =
                (List<Map<String, Object>>) result.getOutputData().get("reranked");
        assertEquals(3, reranked.size());
    }

    private List<Map<String, Object>> buildCandidates() {
        List<Map<String, Object>> candidates = new ArrayList<>();
        candidates.add(new HashMap<>(Map.of("id", "doc-A",
                "text", "Cross-encoder models score query-document pairs jointly for higher accuracy than bi-encoders.",
                "biEncoderScore", 0.91)));
        candidates.add(new HashMap<>(Map.of("id", "doc-B",
                "text", "Re-ranking improves RAG precision by filtering initial retrieval results with a second model.",
                "biEncoderScore", 0.88)));
        candidates.add(new HashMap<>(Map.of("id", "doc-C",
                "text", "Bi-encoder models embed queries and documents independently, enabling fast vector search.",
                "biEncoderScore", 0.86)));
        candidates.add(new HashMap<>(Map.of("id", "doc-D",
                "text", "The Conductor task queue manages worker polling and result acknowledgment.",
                "biEncoderScore", 0.82)));
        candidates.add(new HashMap<>(Map.of("id", "doc-E",
                "text", "HNSW indexes provide logarithmic query time for approximate nearest neighbor search.",
                "biEncoderScore", 0.79)));
        candidates.add(new HashMap<>(Map.of("id", "doc-F",
                "text", "Cohere Rerank API and cross-encoder/ms-marco-MiniLM-L-6-v2 are popular reranking models.",
                "biEncoderScore", 0.76)));
        return candidates;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
