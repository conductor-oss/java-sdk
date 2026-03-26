package recommendationengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Personalizes the top-ranked products for the user's context,
 * returning the top 3 with a reason string.
 */
public class PersonalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rec_personalize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> ranked = (List<Map<String, Object>>) task.getInputData().get("rankedProducts");
        if (ranked == null) ranked = List.of();

        String context = (String) task.getInputData().get("context");
        if (context == null) context = "homepage";

        System.out.println("  [personalize] Personalizing " + ranked.size() + " items for context: " + context);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        int limit = Math.min(ranked.size(), 3);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> p = ranked.get(i);
            String category = (String) p.getOrDefault("category", "general");
            recommendations.add(Map.of(
                    "sku", p.getOrDefault("sku", ""),
                    "score", p.getOrDefault("score", 0.0),
                    "reason", "Similar to your recent " + category + " interests"
            ));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recommendations", recommendations);
        result.getOutputData().put("context", context);
        return result;
    }
}
