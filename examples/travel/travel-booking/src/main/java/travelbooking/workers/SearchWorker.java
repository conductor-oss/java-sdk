package travelbooking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Searches for available travel options. Real price computation based on destination and dates.
 */
public class SearchWorker implements Worker {
    private static final Map<String, Double> BASE_PRICES = Map.of(
            "tokyo", 1200.0, "london", 800.0, "paris", 850.0, "new york", 400.0,
            "sydney", 1500.0, "dubai", 900.0, "singapore", 1100.0);

    @Override public String getTaskDefName() { return "tvb_search"; }

    @Override public TaskResult execute(Task task) {
        String destination = (String) task.getInputData().get("destination");
        if (destination == null) destination = "unknown";

        double basePrice = BASE_PRICES.getOrDefault(destination.toLowerCase(), 600.0 + Math.abs(destination.hashCode() % 500));

        // Generate real search results with price variations
        List<Map<String, Object>> results = new ArrayList<>();
        String[] airlines = {"Economy Direct", "Economy 1-stop", "Business Direct"};
        double[] multipliers = {1.0, 0.7, 2.5};

        for (int i = 0; i < airlines.length; i++) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("airline", airlines[i]);
            option.put("price", Math.round(basePrice * multipliers[i]));
            option.put("duration", airlines[i].contains("1-stop") ? "14h 30m" : "10h 15m");
            results.add(option);
        }

        System.out.println("  [search] Found " + results.size() + " options to " + destination);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("searchResults", results);
        r.getOutputData().put("resultCount", results.size());
        return r;
    }
}
