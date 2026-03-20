package usageanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uag_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [aggregate] Aggregated by user, tower, and time window");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregates", java.util.Map.of("totalMinutes", 4500000, "totalData", "85TB"));
        result.getOutputData().put("anomalies", java.util.List.of("spike-TWR-99"));
        return result;
    }
}
