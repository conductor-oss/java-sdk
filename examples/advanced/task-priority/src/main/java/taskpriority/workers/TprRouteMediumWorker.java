package taskpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TprRouteMediumWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tpr_route_medium";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [route-medium] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queue", "medium_priority");
        result.getOutputData().put("acknowledged", true);
        result.getOutputData().put("slaMinutes", task.getInputData().getOrDefault("sla", 60));
        return result;
    }
}