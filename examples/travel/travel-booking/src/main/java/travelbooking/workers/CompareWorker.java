package travelbooking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Compares search results and selects best option. Real comparison logic.
 */
public class CompareWorker implements Worker {
    @Override public String getTaskDefName() { return "tvb_compare"; }

    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object resultsObj = task.getInputData().get("searchResults");
        Object budgetObj = task.getInputData().get("budget");
        double budget = budgetObj instanceof Number ? ((Number) budgetObj).doubleValue() : Double.MAX_VALUE;

        List<Map<String, Object>> results = new ArrayList<>();
        if (resultsObj instanceof List) {
            for (Object o : (List<?>) resultsObj) {
                if (o instanceof Map) results.add((Map<String, Object>) o);
            }
        }

        // Find cheapest within budget
        Map<String, Object> best = null;
        for (Map<String, Object> r : results) {
            double price = r.get("price") instanceof Number ? ((Number) r.get("price")).doubleValue() : Double.MAX_VALUE;
            if (price <= budget && (best == null || price < ((Number) best.get("price")).doubleValue())) {
                best = r;
            }
        }

        boolean withinBudget = best != null;

        System.out.println("  [compare] " + results.size() + " options compared, best: "
                + (best != null ? best.get("airline") + " $" + best.get("price") : "none within budget"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestOption", best != null ? best : Map.of());
        result.getOutputData().put("withinBudget", withinBudget);
        result.getOutputData().put("optionsCompared", results.size());
        return result;
    }
}
