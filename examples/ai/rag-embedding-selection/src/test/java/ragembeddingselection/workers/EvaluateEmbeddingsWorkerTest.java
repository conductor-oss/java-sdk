package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluateEmbeddingsWorkerTest {

    private final EvaluateEmbeddingsWorker worker = new EvaluateEmbeddingsWorker();

    @Test
    void taskDefName() {
        assertEquals("es_evaluate_embeddings", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void computesRankingsAndEvaluation() {
        Map<String, Object> openai = new HashMap<>(Map.of(
                "model", "openai/text-embedding-3-large",
                "dimensions", 3072,
                "precisionAt1", 0.93,
                "precisionAt3", 0.89,
                "recallAt5", 0.95,
                "ndcg", 0.91,
                "latencyMs", 120,
                "costPerQuery", 0.00013
        ));
        Map<String, Object> cohere = new HashMap<>(Map.of(
                "model", "cohere/embed-english-v3.0",
                "dimensions", 1024,
                "precisionAt1", 0.90,
                "precisionAt3", 0.87,
                "recallAt5", 0.92,
                "ndcg", 0.88,
                "latencyMs", 95,
                "costPerQuery", 0.0001
        ));
        Map<String, Object> local = new HashMap<>(Map.of(
                "model", "local/all-MiniLM-L6-v2",
                "dimensions", 384,
                "precisionAt1", 0.82,
                "precisionAt3", 0.78,
                "recallAt5", 0.86,
                "ndcg", 0.80,
                "latencyMs", 15,
                "costPerQuery", 0.0
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "openaiMetrics", openai,
                "cohereMetrics", cohere,
                "localMetrics", local
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> rankings = (List<Map<String, Object>>) result.getOutputData().get("rankings");
        assertNotNull(rankings);
        assertEquals(3, rankings.size());

        // First ranked should be openai (highest composite)
        assertEquals("openai/text-embedding-3-large", rankings.get(0).get("model"));

        // Verify composite scores are in descending order
        double first = EvaluateEmbeddingsWorker.toDouble(rankings.get(0).get("compositeScore"));
        double second = EvaluateEmbeddingsWorker.toDouble(rankings.get(1).get("compositeScore"));
        double third = EvaluateEmbeddingsWorker.toDouble(rankings.get(2).get("compositeScore"));
        assertTrue(first >= second);
        assertTrue(second >= third);

        Map<String, Object> evaluation = (Map<String, Object>) result.getOutputData().get("evaluation");
        assertNotNull(evaluation);
        assertEquals("openai/text-embedding-3-large", evaluation.get("bestQuality"));
        assertEquals("local/all-MiniLM-L6-v2", evaluation.get("fastestLatency"));
        assertEquals("local/all-MiniLM-L6-v2", evaluation.get("lowestCost"));
    }

    @Test
    void compositeScoreFormula() {
        // ndcg*0.4 + recall*0.3 + precision*0.2 + (1 - latency/200)*0.1
        // openai: 0.91*0.4 + 0.95*0.3 + 0.93*0.2 + (1-120/200)*0.1
        //       = 0.364 + 0.285 + 0.186 + 0.04 = 0.875
        double expected = 0.91 * 0.4 + 0.95 * 0.3 + 0.93 * 0.2 + (1.0 - 120.0 / 200.0) * 0.1;
        double rounded = EvaluateEmbeddingsWorker.round(expected, 4);

        Map<String, Object> openai = new HashMap<>(Map.of(
                "model", "openai/text-embedding-3-large",
                "dimensions", 3072,
                "precisionAt1", 0.93,
                "precisionAt3", 0.89,
                "recallAt5", 0.95,
                "ndcg", 0.91,
                "latencyMs", 120,
                "costPerQuery", 0.00013
        ));
        Map<String, Object> cohere = new HashMap<>(Map.of(
                "model", "cohere/embed-english-v3.0",
                "dimensions", 1024,
                "precisionAt1", 0.90,
                "precisionAt3", 0.87,
                "recallAt5", 0.92,
                "ndcg", 0.88,
                "latencyMs", 95,
                "costPerQuery", 0.0001
        ));
        Map<String, Object> local = new HashMap<>(Map.of(
                "model", "local/all-MiniLM-L6-v2",
                "dimensions", 384,
                "precisionAt1", 0.82,
                "precisionAt3", 0.78,
                "recallAt5", 0.86,
                "ndcg", 0.80,
                "latencyMs", 15,
                "costPerQuery", 0.0
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "openaiMetrics", openai,
                "cohereMetrics", cohere,
                "localMetrics", local
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rankings = (List<Map<String, Object>>) result.getOutputData().get("rankings");
        // OpenAI should be first
        assertEquals(rounded, rankings.get(0).get("compositeScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
