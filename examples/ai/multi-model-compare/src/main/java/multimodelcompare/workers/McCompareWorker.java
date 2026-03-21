package multimodelcompare.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares results from GPT-4, Claude, and Gemini model calls.
 * Determines the winner (highest quality), fastest, cheapest, and best quality models.
 */
public class McCompareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mc_compare";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();

        Map<String, Object> gpt4 = (Map<String, Object>) input.get("gpt4");
        Map<String, Object> claude = (Map<String, Object>) input.get("claude");
        Map<String, Object> gemini = (Map<String, Object>) input.get("gemini");

        List<Map<String, Object>> models = List.of(gpt4, claude, gemini);

        // Build scores list sorted by quality descending
        List<Map<String, Object>> scores = new ArrayList<>();
        for (Map<String, Object> m : models) {
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("model", m.get("model"));
            score.put("quality", ((Number) m.get("quality")).doubleValue());
            score.put("latencyMs", ((Number) m.get("latencyMs")).intValue());
            score.put("cost", ((Number) m.get("cost")).doubleValue());
            scores.add(score);
        }
        scores.sort(Comparator.comparingDouble(
                (Map<String, Object> s) -> ((Number) s.get("quality")).doubleValue()).reversed());

        // Find fastest (lowest latency)
        String fastest = models.stream()
                .min(Comparator.comparingInt(m -> ((Number) m.get("latencyMs")).intValue()))
                .map(m -> (String) m.get("model"))
                .orElse("");

        // Find cheapest (lowest cost)
        String cheapest = models.stream()
                .min(Comparator.comparingDouble(m -> ((Number) m.get("cost")).doubleValue()))
                .map(m -> (String) m.get("model"))
                .orElse("");

        // Find best quality (highest quality)
        String bestQuality = models.stream()
                .max(Comparator.comparingDouble(m -> ((Number) m.get("quality")).doubleValue()))
                .map(m -> (String) m.get("model"))
                .orElse("");

        // Winner = highest quality
        String winner = bestQuality;

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("fastest", fastest);
        comparison.put("cheapest", cheapest);
        comparison.put("bestQuality", bestQuality);

        System.out.println("  [mc_compare worker] Winner: " + winner);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("winner", winner);
        result.getOutputData().put("scores", scores);
        result.getOutputData().put("comparison", comparison);
        return result;
    }
}
