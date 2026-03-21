package gracefulserviceshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checkpoints the current state of the service instance before shutdown.
 * Input: instanceId, pendingTasks
 * Output: saved (boolean), instanceId
 */
public class CheckpointWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gs_checkpoint";
    }

    @Override
    public TaskResult execute(Task task) {
        String instanceId = (String) task.getInputData().get("instanceId");
        if (instanceId == null) {
            instanceId = "unknown-instance";
        }

        Object pendingTasks = task.getInputData().get("pendingTasks");
        int pending = 0;
        if (pendingTasks instanceof Number) {
            pending = ((Number) pendingTasks).intValue();
        }

        System.out.println("  [checkpoint] State saved for " + instanceId + " (pending=" + pending + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("saved", true);
        result.getOutputData().put("instanceId", instanceId);
        result.getOutputData().put("pendingTasks", pending);
        return result;
    }
}
