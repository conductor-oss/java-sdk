package priceoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects market data: competitor prices, market trend, average market price, supply level.
 */
public class CollectMarketDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prz_collect_market_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null) productId = "unknown";
        String category = (String) task.getInputData().get("category");
        if (category == null) category = "general";

        System.out.println("  [market] Collecting data for " + productId + " in " + category);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("competitorPrices", List.of(89.99, 94.50, 87.00, 92.75));
        result.getOutputData().put("marketTrend", "upward");
        result.getOutputData().put("avgMarketPrice", 91.06);
        result.getOutputData().put("supplyLevel", "moderate");
        return result;
    }
}
