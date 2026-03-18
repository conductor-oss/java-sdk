package scattergather.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates responses from all gather workers and finds the best (lowest) price.
 * Performs real comparison logic across all responses.
 *
 * Input: response1, response2, response3
 * Output: aggregated (list), bestPrice (map), responseCount (int)
 */
public class SgrAggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgr_aggregate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> allResponses = new ArrayList<>();

        for (String key : List.of("response1", "response2", "response3")) {
            Object respObj = task.getInputData().get(key);
            if (respObj instanceof Map) {
                allResponses.add((Map<String, Object>) respObj);
            }
        }

        // Find the best (lowest) price
        Map<String, Object> bestPrice = null;
        double lowestPrice = Double.MAX_VALUE;

        for (Map<String, Object> resp : allResponses) {
            Object priceObj = resp.get("price");
            double price = Double.MAX_VALUE;
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            }
            if (price < lowestPrice) {
                lowestPrice = price;
                bestPrice = new HashMap<>();
                bestPrice.put("source", resp.get("source"));
                bestPrice.put("price", price);
            }
        }

        if (bestPrice == null) {
            bestPrice = Map.of("source", "none", "price", 0.0);
        }

        System.out.println("  [aggregate] Aggregated " + allResponses.size() + " responses, best price: "
                + bestPrice.get("price") + " from " + bestPrice.get("source"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregated", allResponses);
        result.getOutputData().put("bestPrice", bestPrice);
        result.getOutputData().put("responseCount", allResponses.size());
        return result;
    }
}
