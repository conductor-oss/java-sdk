package ragembeddingselection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that evaluates embedding metrics from all providers.
 * Computes a composite score for each model and determines the best
 * by quality, latency, and cost.
 *
 * Composite score = ndcg*0.4 + recall*0.3 + precision*0.2 + (1 - latency/200)*0.1
 */
public class EvaluateEmbeddingsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_evaluate_embeddings";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> openaiMetrics = (Map<String, Object>) task.getInputData().get("openaiMetrics");
        Map<String, Object> cohereMetrics = (Map<String, Object>) task.getInputData().get("cohereMetrics");
        Map<String, Object> localMetrics = (Map<String, Object>) task.getInputData().get("localMetrics");

        List<Map<String, Object>> allMetrics = List.of(openaiMetrics, cohereMetrics, localMetrics);

        List<Map<String, Object>> rankings = new ArrayList<>();
        for (Map<String, Object> m : allMetrics) {
            double ndcg = toDouble(m.get("ndcg"));
            double recall = toDouble(m.get("recallAt5"));
            double precision = toDouble(m.get("precisionAt1"));
            int latency = toInt(m.get("latencyMs"));

            double compositeScore = ndcg * 0.4
                    + recall * 0.3
                    + precision * 0.2
                    + (1.0 - latency / 200.0) * 0.1;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("model", m.get("model"));
            entry.put("compositeScore", round(compositeScore, 4));
            entry.put("ndcg", ndcg);
            entry.put("recallAt5", recall);
            entry.put("precisionAt1", precision);
            entry.put("latencyMs", latency);
            entry.put("costPerQuery", toDouble(m.get("costPerQuery")));
            rankings.add(entry);
        }

        rankings.sort(Comparator.comparingDouble(
                (Map<String, Object> e) -> toDouble(e.get("compositeScore"))).reversed());

        // Find best by quality (highest composite), fastest latency, lowest cost
        Map<String, Object> bestQuality = rankings.get(0);

        Map<String, Object> fastestLatency = rankings.stream()
                .min(Comparator.comparingInt(e -> toInt(e.get("latencyMs"))))
                .orElse(rankings.get(0));

        Map<String, Object> lowestCost = rankings.stream()
                .min(Comparator.comparingDouble(e -> toDouble(e.get("costPerQuery"))))
                .orElse(rankings.get(0));

        Map<String, Object> evaluation = new LinkedHashMap<>();
        evaluation.put("bestQuality", bestQuality.get("model"));
        evaluation.put("bestQualityScore", bestQuality.get("compositeScore"));
        evaluation.put("fastestLatency", fastestLatency.get("model"));
        evaluation.put("fastestLatencyMs", fastestLatency.get("latencyMs"));
        evaluation.put("lowestCost", lowestCost.get("model"));
        evaluation.put("lowestCostPerQuery", lowestCost.get("costPerQuery"));

        System.out.println("  [evaluate] Rankings: "
                + rankings.stream().map(r -> r.get("model") + "=" + r.get("compositeScore")).toList());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rankings", rankings);
        result.getOutputData().put("evaluation", evaluation);
        return result;
    }

    static double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    static int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    static double round(double value, int places) {
        double factor = 1;
        for (int i = 0; i < places; i++) factor *= 10;
        return Math.round(value * factor) / factor;
    }
}
