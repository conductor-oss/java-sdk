package monitoringalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RouteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ma_route";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [route] Sent to on-call via Slack");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("notified", true);
        return result;
    }
}
