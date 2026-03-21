package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectBestWorkerTest {

    private final SelectBestWorker worker = new SelectBestWorker();

    @Test
    void taskDefName() {
        assertEquals("es_select_best", worker.getTaskDefName());
    }

    @Test
    void selectsBestModelFromRankings() {
        List<Map<String, Object>> rankings = new ArrayList<>();

        Map<String, Object> first = new LinkedHashMap<>();
        first.put("model", "openai/text-embedding-3-large");
        first.put("compositeScore", 0.875);
        first.put("latencyMs", 120);
        first.put("costPerQuery", 0.00013);
        rankings.add(first);

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("model", "cohere/embed-english-v3.0");
        second.put("compositeScore", 0.8585);
        second.put("latencyMs", 95);
        second.put("costPerQuery", 0.0001);
        rankings.add(second);

        Map<String, Object> third = new LinkedHashMap<>();
        third.put("model", "local/all-MiniLM-L6-v2");
        third.put("compositeScore", 0.8145);
        third.put("latencyMs", 15);
        third.put("costPerQuery", 0.0);
        rankings.add(third);

        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("bestQuality", "openai/text-embedding-3-large");
        evaluation.put("bestQualityScore", 0.875);
        evaluation.put("fastestLatency", "local/all-MiniLM-L6-v2");
        evaluation.put("fastestLatencyMs", 15);
        evaluation.put("lowestCost", "local/all-MiniLM-L6-v2");
        evaluation.put("lowestCostPerQuery", 0.0);

        Task task = taskWith(new HashMap<>(Map.of(
                "rankings", rankings,
                "evaluation", evaluation
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("openai/text-embedding-3-large", result.getOutputData().get("bestModel"));
        assertEquals(0.875, result.getOutputData().get("bestScore"));

        String recommendation = (String) result.getOutputData().get("recommendation");
        assertNotNull(recommendation);
        assertTrue(recommendation.contains("openai/text-embedding-3-large"));
        assertTrue(recommendation.contains("local/all-MiniLM-L6-v2"));
    }

    @Test
    void recommendationContainsAllCategories() {
        List<Map<String, Object>> rankings = new ArrayList<>();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("model", "cohere/embed-english-v3.0");
        entry.put("compositeScore", 0.86);
        rankings.add(entry);

        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("bestQuality", "cohere/embed-english-v3.0");
        evaluation.put("fastestLatency", "local/all-MiniLM-L6-v2");
        evaluation.put("lowestCost", "local/all-MiniLM-L6-v2");

        Task task = taskWith(new HashMap<>(Map.of(
                "rankings", rankings,
                "evaluation", evaluation
        )));
        TaskResult result = worker.execute(task);

        String rec = (String) result.getOutputData().get("recommendation");
        assertTrue(rec.contains("Recommended:"));
        assertTrue(rec.contains("Best quality:"));
        assertTrue(rec.contains("Fastest:"));
        assertTrue(rec.contains("Cheapest:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
