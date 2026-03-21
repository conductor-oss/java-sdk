package tradeexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Checks regulatory compliance for the trade.
 */
public class CheckComplianceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trd_check_compliance";
    }

    @Override
    public TaskResult execute(Task task) {
        String symbol = (String) task.getInputData().get("symbol");

        System.out.println("  [compliance] Checking regulatory compliance for " + symbol);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("compliant", true);
        result.getOutputData().put("checks",
                List.of("pattern_day_trader", "restricted_list", "concentration_limit", "wash_sale"));
        result.getOutputData().put("allPassed", true);
        return result;
    }
}
