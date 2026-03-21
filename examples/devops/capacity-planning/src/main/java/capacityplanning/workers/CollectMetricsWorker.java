package capacityplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_collect_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [collect] 30 days of metrics collected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("dataPoints", 43200);
        return result;
    }
}
