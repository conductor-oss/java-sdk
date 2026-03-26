package regulatoryreporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "reg_collect_data"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Gathering " + task.getInputData().get("reportType") + " data for " + task.getInputData().get("reportingPeriod"));
        result.getOutputData().put("data", Map.of("totalAssets", 2500000000L, "totalLiabilities", 1800000000L, "capitalRatio", 14.2, "riskWeightedAssets", 1900000000L, "netIncome", 85000000L));
        result.getOutputData().put("recordCount", 45280);
        return result;
    }
}
