package taskpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TprRouteLowWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tpr_route_low";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [route-low] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queue", "low_priority");
        result.getOutputData().put("acknowledged", true);
        result.getOutputData().put("slaMinutes", task.getInputData().getOrDefault("sla", 240));
        return result;
    }
}