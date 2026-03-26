package priceoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Analyzes demand for a product: demand score, elasticity, seasonal factor, forecasted demand.
 */
public class AnalyzeDemandWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prz_analyze_demand";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null) productId = "unknown";

        System.out.println("  [demand] Analyzing demand for " + productId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("demandScore", 0.78);
        result.getOutputData().put("elasticity", -1.2);
        result.getOutputData().put("seasonalFactor", 1.15);
        result.getOutputData().put("forecastedDemand", 340);
        return result;
    }
}
