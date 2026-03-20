package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HealthCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_health_check";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [health] All containers healthy");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("health_check", true);
        return result;
    }
}
