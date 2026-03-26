package observabilitypipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "op_collect_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [collect] Collected 15,000 metrics from checkout-service");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_metricsId", "COLLECT_METRICS-1365");
        result.addOutputData("success", true);
        return result;
    }
}
