package riskassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Collects risk factors for the portfolio.
 */
public class CollectFactorsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rsk_collect_factors";
    }

    @Override
    public TaskResult execute(Task task) {
        String portfolioId = (String) task.getInputData().get("portfolioId");

        System.out.println("  [collect] Gathering risk factors for portfolio " + portfolioId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("marketData",
                Map.of("volatility", 18.5, "beta", 1.2, "correlation", 0.75));
        result.getOutputData().put("creditData",
                Map.of("avgRating", "BBB+", "defaultRate", 0.5, "concentration", 15));
        result.getOutputData().put("operationalData",
                Map.of("incidents", 2, "controlGaps", 3, "processMaturity", 72));
        return result;
    }
}
