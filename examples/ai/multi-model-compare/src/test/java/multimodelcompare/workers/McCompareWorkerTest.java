package multimodelcompare.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McCompareWorkerTest {

    private final McCompareWorker worker = new McCompareWorker();

    @Test
    void taskDefName() {
        assertEquals("mc_compare", worker.getTaskDefName());
    }

    @Test
    void comparesModelsAndPicksWinner() {
        Map<String, Object> gpt4 = new HashMap<>(Map.of(
                "model", "gpt-4",
                "response", "GPT-4 response",
                "latencyMs", 1200,
                "tokens", 85,
                "cost", 0.0051,
                "quality", 9.2
        ));
        Map<String, Object> claude = new HashMap<>(Map.of(
                "model", "claude-3",
                "response", "Claude response",
                "latencyMs", 980,
                "tokens", 92,
                "cost", 0.0069,
                "quality", 9.0
        ));
        Map<String, Object> gemini = new HashMap<>(Map.of(
                "model", "gemini-pro",
                "response", "Gemini response",
                "latencyMs", 750,
                "tokens", 78,
                "cost", 0.0001,
                "quality", 8.5
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4", gpt4,
                "claude", claude,
                "gemini", gemini
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gpt-4", result.getOutputData().get("winner"));
    }

    @Test
    void scoresAreSortedByQualityDescending() {
        Map<String, Object> gpt4 = new HashMap<>(Map.of(
                "model", "gpt-4",
                "latencyMs", 1200,
                "tokens", 85,
                "cost", 0.0051,
                "quality", 9.2
        ));
        Map<String, Object> claude = new HashMap<>(Map.of(
                "model", "claude-3",
                "latencyMs", 980,
                "tokens", 92,
                "cost", 0.0069,
                "quality", 9.0
        ));
        Map<String, Object> gemini = new HashMap<>(Map.of(
                "model", "gemini-pro",
                "latencyMs", 750,
                "tokens", 78,
                "cost", 0.0001,
                "quality", 8.5
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4", gpt4,
                "claude", claude,
                "gemini", gemini
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) result.getOutputData().get("scores");
        assertEquals(3, scores.size());
        assertEquals("gpt-4", scores.get(0).get("model"));
        assertEquals("claude-3", scores.get(1).get("model"));
        assertEquals("gemini-pro", scores.get(2).get("model"));

        double first = ((Number) scores.get(0).get("quality")).doubleValue();
        double second = ((Number) scores.get(1).get("quality")).doubleValue();
        double third = ((Number) scores.get(2).get("quality")).doubleValue();
        assertTrue(first >= second);
        assertTrue(second >= third);
    }

    @Test
    void comparisonIdentifiesFastestCheapestBestQuality() {
        Map<String, Object> gpt4 = new HashMap<>(Map.of(
                "model", "gpt-4",
                "latencyMs", 1200,
                "tokens", 85,
                "cost", 0.0051,
                "quality", 9.2
        ));
        Map<String, Object> claude = new HashMap<>(Map.of(
                "model", "claude-3",
                "latencyMs", 980,
                "tokens", 92,
                "cost", 0.0069,
                "quality", 9.0
        ));
        Map<String, Object> gemini = new HashMap<>(Map.of(
                "model", "gemini-pro",
                "latencyMs", 750,
                "tokens", 78,
                "cost", 0.0001,
                "quality", 8.5
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4", gpt4,
                "claude", claude,
                "gemini", gemini
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");
        assertEquals("gemini-pro", comparison.get("fastest"));
        assertEquals("gemini-pro", comparison.get("cheapest"));
        assertEquals("gpt-4", comparison.get("bestQuality"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Map<String, Object> gpt4 = new HashMap<>(Map.of(
                "model", "gpt-4",
                "latencyMs", 1200,
                "tokens", 85,
                "cost", 0.0051,
                "quality", 9.2
        ));
        Map<String, Object> claude = new HashMap<>(Map.of(
                "model", "claude-3",
                "latencyMs", 980,
                "tokens", 92,
                "cost", 0.0069,
                "quality", 9.0
        ));
        Map<String, Object> gemini = new HashMap<>(Map.of(
                "model", "gemini-pro",
                "latencyMs", 750,
                "tokens", 78,
                "cost", 0.0001,
                "quality", 8.5
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4", gpt4,
                "claude", claude,
                "gemini", gemini
        )));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("winner"));
        assertTrue(result.getOutputData().containsKey("scores"));
        assertTrue(result.getOutputData().containsKey("comparison"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
