package featurestore.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FstComputeFeaturesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fst_compute_features";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [compute] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("features", java.util.List.of(java.util.Map.of("name","avg_purchase_amount_30d","type","float")));
        result.getOutputData().put("featureCount", 5);
        result.getOutputData().put("stats", java.util.Map.of("rowCount", 125000, "nullRate", 0.02));
        return result;
    }
}