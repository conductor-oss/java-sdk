package recommendationengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ranks candidate products by similarity score and assigns a rank number.
 */
public class RankCandidatesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rec_rank_candidates";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) task.getInputData().get("similarProducts");
        if (products == null) products = List.of();

        System.out.println("  [rank] Ranking " + products.size() + " candidate products");

        List<Map<String, Object>> sorted = new ArrayList<>(products);
        sorted.sort(Comparator.comparingDouble((Map<String, Object> m) -> {
            Object s = m.get("score");
            return s instanceof Number ? ((Number) s).doubleValue() : 0.0;
        }).reversed());

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map<String, Object> entry = new LinkedHashMap<>(sorted.get(i));
            entry.put("rank", i + 1);
            ranked.add(entry);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rankedProducts", ranked);
        result.getOutputData().put("totalCandidates", ranked.size());
        return result;
    }
}
