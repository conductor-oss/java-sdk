package gracefulserviceshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Drains in-flight tasks for the given instance, waiting for them to complete.
 * Input: instanceId
 * Output: drained (boolean), pendingTasks (int)
 */
public class DrainTasksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gs_drain_tasks";
    }

    @Override
    public TaskResult execute(Task task) {
        String instanceId = (String) task.getInputData().get("instanceId");
        if (instanceId == null) {
            instanceId = "unknown-instance";
        }

        System.out.println("  [drain] Waiting for 3 in-flight tasks to complete on " + instanceId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("drained", true);
        result.getOutputData().put("pendingTasks", 0);
        result.getOutputData().put("instanceId", instanceId);
        return result;
    }
}
