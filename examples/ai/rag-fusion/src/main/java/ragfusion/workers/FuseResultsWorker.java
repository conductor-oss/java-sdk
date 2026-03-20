package ragfusion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that fuses results from multiple search engines using Reciprocal Rank
 * Fusion (RRF). Uses k=60, score = sum(1/(k+rank)) across all result lists.
 * Returns fusedDocs sorted by RRF score descending, fusedCount, totalCandidates.
 */
public class FuseResultsWorker implements Worker {

    private static final int K = 60;

    @Override
    public String getTaskDefName() {
        return "rf_fuse_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> results1 = (List<Map<String, Object>>) task.getInputData().get("results1");
        List<Map<String, Object>> results2 = (List<Map<String, Object>>) task.getInputData().get("results2");
        List<Map<String, Object>> results3 = (List<Map<String, Object>>) task.getInputData().get("results3");

        if (results1 == null) results1 = List.of();
        if (results2 == null) results2 = List.of();
        if (results3 == null) results3 = List.of();

        int totalCandidates = results1.size() + results2.size() + results3.size();

        // RRF scoring: accumulate 1/(k+rank) for each document across all result lists
        // Use document id as the key for deduplication
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, String> texts = new HashMap<>();

        for (List<Map<String, Object>> resultList : List.of(results1, results2, results3)) {
            for (Map<String, Object> doc : resultList) {
                String id = (String) doc.get("id");
                String text = (String) doc.get("text");
                int rank = ((Number) doc.get("rank")).intValue();

                double rrfScore = 1.0 / (K + rank);
                scores.merge(id, rrfScore, Double::sum);
                texts.putIfAbsent(id, text);
            }
        }

        // Sort by RRF score descending
        List<Map<String, Object>> fusedDocs = new ArrayList<>();
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> {
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("id", entry.getKey());
                    doc.put("text", texts.get(entry.getKey()));
                    doc.put("rrfScore", Math.round(entry.getValue() * 100000.0) / 100000.0);
                    fusedDocs.add(doc);
                });

        System.out.println("  [fuse] Fused " + totalCandidates + " candidates into " + fusedDocs.size() + " unique docs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fusedDocs", fusedDocs);
        result.getOutputData().put("fusedCount", fusedDocs.size());
        result.getOutputData().put("totalCandidates", totalCandidates);
        return result;
    }
}
