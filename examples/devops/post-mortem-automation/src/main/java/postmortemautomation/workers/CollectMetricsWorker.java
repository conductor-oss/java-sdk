package postmortemautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_collect_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [metrics] Impact: 1,200 affected users, 99.2% availability during incident");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_metrics", true);
        result.addOutputData("processed", true);
        return result;
    }
}
